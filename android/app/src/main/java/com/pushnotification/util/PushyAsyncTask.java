package com.pushnotification.util;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PushyAsyncTask<Params, Progress, Result> {
  private static final String LOG_TAG = "AsyncTaskAdapter";
  
  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
  
  private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
  
  private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
  
  private static final int KEEP_ALIVE = 1;
  
  private static final ThreadFactory sThreadFactory = new ThreadFactory() {
      private final AtomicInteger mCount = new AtomicInteger(1);
      
      public Thread newThread(Runnable r) {
        return new Thread(r, "AsyncTaskAdapter #" + this.mCount.getAndIncrement());
      }
    };
  
  private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(128);
  
  public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 1L, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
  
  private static final int MESSAGE_POST_RESULT = 1;
  
  private static final int MESSAGE_POST_PROGRESS = 2;
  
  private static final InternalHandler sHandler = new InternalHandler();
  
  private static volatile Executor sDefaultExecutor = THREAD_POOL_EXECUTOR;
  
  private final WorkerRunnable<Params, Result> mWorker;
  
  private final FutureTask<Result> mFuture;
  
  private final AtomicBoolean mCancelled = new AtomicBoolean();
  
  private final AtomicBoolean mTaskInvoked = new AtomicBoolean();
  
  private volatile Status mStatus = Status.PENDING;
  
  public PushyAsyncTask() {
    this.mWorker = new WorkerRunnable<Params, Result>() {
        public Result call() throws Exception {
          PushyAsyncTask.this.mTaskInvoked.set(true);
          Process.setThreadPriority(10);
          return PushyAsyncTask.this.postResult(PushyAsyncTask.this.doInBackground(this.mParams));
        }
      };
    this.mFuture = new FutureTask<Result>(this.mWorker) {
        protected void done() {
          try {
            PushyAsyncTask.this.postResultIfNotInvoked(get());
          } catch (InterruptedException e) {
            Log.w("AsyncTaskAdapter", e);
          } catch (ExecutionException e) {
            throw new RuntimeException("An error occured while executing doInBackground()", e
                .getCause());
          } catch (CancellationException e) {
            PushyAsyncTask.this.postResultIfNotInvoked(null);
          } 
        }
      };
  }
  
  public static void init() {
    sHandler.getLooper();
  }
  
  public static void setDefaultExecutor(Executor exec) {
    sDefaultExecutor = exec;
  }
  
  public static void execute(Runnable runnable) {
    sDefaultExecutor.execute(runnable);
  }
  
  private void postResultIfNotInvoked(Result result) {
    boolean wasTaskInvoked = this.mTaskInvoked.get();
    if (!wasTaskInvoked)
      postResult(result); 
  }
  
  private Result postResult(Result result) {
    Message message = sHandler.obtainMessage(1, new AsyncTaskAdapterResult(this, new Object[] { result }));
    message.sendToTarget();
    return result;
  }
  
  public final Status getStatus() {
    return this.mStatus;
  }
  
  protected void onPreExecute() {}
  
  protected void onPostExecute(Result result) {}
  
  protected void onProgressUpdate(Progress... values) {}
  
  protected void onCancelled(Result result) {
    onCancelled();
  }
  
  protected void onCancelled() {}
  
  public final boolean isCancelled() {
    return this.mCancelled.get();
  }
  
  public final boolean cancel(boolean mayInterruptIfRunning) {
    this.mCancelled.set(true);
    return this.mFuture.cancel(mayInterruptIfRunning);
  }
  
  public final Result get() throws InterruptedException, ExecutionException {
    return this.mFuture.get();
  }
  
  public final Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.mFuture.get(timeout, unit);
  }
  
  public final PushyAsyncTask<Params, Progress, Result> execute(Params... params) {
    return executeOnExecutor(sDefaultExecutor, params);
  }
  
  public final PushyAsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
    if (this.mStatus != Status.PENDING)
      switch (this.mStatus) {
        case RUNNING:
          throw new IllegalStateException("Cannot execute task: the task is already running.");
        case FINISHED:
          throw new IllegalStateException("Cannot execute task: the task has already been executed (a task can be executed only once)");
      }  
    this.mStatus = Status.RUNNING;
    onPreExecute();
    this.mWorker.mParams = params;
    exec.execute(this.mFuture);
    return this;
  }
  
  protected final void publishProgress(Progress... values) {
    if (!isCancelled())
      sHandler.obtainMessage(2, new AsyncTaskAdapterResult<>(this, values))
        .sendToTarget(); 
  }
  
  private void finish(Result result) {
    if (isCancelled()) {
      onCancelled(result);
    } else {
      onPostExecute(result);
    } 
    this.mStatus = Status.FINISHED;
  }
  
  protected abstract Result doInBackground(Params... paramVarArgs);
  
  public enum Status {
    PENDING, RUNNING, FINISHED;
  }
  
  private static class InternalHandler extends Handler {
    private InternalHandler() {}
    
    public void handleMessage(Message msg) {
      PushyAsyncTask.AsyncTaskAdapterResult result = (PushyAsyncTask.AsyncTaskAdapterResult)msg.obj;
      switch (msg.what) {
        case 1:
          result.mTask.finish(result.mData[0]);
          break;
        case 2:
          result.mTask.onProgressUpdate(result.mData);
          break;
      } 
    }
  }
  
  private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
    Params[] mParams;
    
    private WorkerRunnable() {}
  }
  
  private static class AsyncTaskAdapterResult<Data> {
    final PushyAsyncTask mTask;
    
    final Data[] mData;
    
    AsyncTaskAdapterResult(PushyAsyncTask task, Data... data) {
      this.mTask = task;
      this.mData = data;
    }
  }
}
