import { DistributionResponse } from '../services/distribution.service';
export const LOCAL_SERVER_ADDRESS = '100.80.89.35:7777';
export const LOCAL_PACKAGE_NAME = 'com.touch.mobile.dark';
export const LOCAL_PROJECT_NAME = 'Ariana Mobile';
export const localDistribution: DistributionResponse = {
  cdnLauncher: '', cdnCache: '', cacheMode: [], rss: '', versionHash: 'local',
  packageName: LOCAL_PACKAGE_NAME, projectName: LOCAL_PROJECT_NAME, filesContinue: [],
  servers: [{ id: 1, show: true, version: '1.0', icon: '', events: [], slot: 1000, bonus: false,
    name: LOCAL_PROJECT_NAME, description: 'Ariana Mobile Server', address: LOCAL_SERVER_ADDRESS, sampVersion: '0.3.7' }],
  launcher: { appVersion: '1.0.0', name: 'local', hash: '', bytes: 0, size: 'local' },
};
