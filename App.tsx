/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * Generated with the TypeScript template
 * https://github.com/react-native-community/react-native-template-typescript
 *
 * @format
 */

import React, { useEffect, useState } from 'react';
import {
  Alert,
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import { Colors } from 'react-native/Libraries/NewAppScreen';

import Pushy from './Pushy';

Pushy.setNotificationListener(async data => {
  // Print notification payload data
  console.log('Received notification: ' + JSON.stringify(data));

  // Notification title
  let notificationTitle = 'Pushy';

  // Attempt to extract the "message" property from the payload: {"message":"Hello World!"}
  let notificationText = data.message || 'Test notification';

  // Display basic system notification
  Pushy.notify(notificationTitle, notificationText, data);
});

// Pushy Notification Click Listener
Pushy.setNotificationClickListener(async data => {
  // Display basic alert
  Alert.alert('Clicked notification: ' + data.message);

  // Navigate the user to another page or
  // execute other logic on notification click
});


const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  const [tokenDisplay, setTokenDisplay] = useState('Registering your device...');
  const [instructions, setInstructions] = useState('(please wait)');

  useEffect(() => {
    // Start the Pushy service
    Pushy.listen();

    // Register the device for push notifications
    Pushy.register()
      .then(async deviceToken => {
        // Update UI
        setTokenDisplay(deviceToken);
        setInstructions('(copy from device logs)');

        // Write device token to device logs
        console.log('Pushy device token: ' + deviceToken);

        // Send the token to your backend server via an HTTP GET request
        //await fetch('https://your.api.hostname/register/device?token=' + deviceToken);

        // Succeeded, optionally do something to alert the user
      })
      .catch(err => {
        // Update UI
        setTokenDisplay('Error');
        setInstructions(err.toString());

        // Handle registration errors
        console.error(err);
      });

    // Android-only code
    if (Platform.OS === 'android') {
      // Set system status bar color
      StatusBar.setBackgroundColor('#000000');
    }

    // Light system status bar text
    StatusBar.setBarStyle('light-content', true);
  });

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <View style={styles.content}>
          <Text style={styles.tokenDisplay}>{tokenDisplay}</Text>
          <Text style={styles.instructions}>{instructions}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center'
  },
  tokenDisplay: {
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
    margin: 10
  },
  instructions: {
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 5
  },
});

export default App;
