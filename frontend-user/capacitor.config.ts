import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.minthive.app',
  appName: 'minthive',
  webDir: 'dist',
  server: {
    // 正式包使用 https scheme，避免 WebView 默认放开明文流量
    androidScheme: 'https',
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
