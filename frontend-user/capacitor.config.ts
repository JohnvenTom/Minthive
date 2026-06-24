import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.minthive.app',
  appName: 'minthive',
  webDir: 'dist',
  server: {
    cleartext: true,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#0D1117',
      showSpinner: false,
    },
  },
};

export default config;
