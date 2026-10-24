import RNGpuInfo from 'react-native-gpu-info';
import { setGPU, setInitial } from '../actions/appActions';
import { AppThunk } from '../store/store';
import { fetchDistribution } from './distributionThunks';
import { fetchPermisions } from './permisionThunks';
import { fetchInitialSettings } from './settingsThunks';
export const fetchInitialApp = (): AppThunk => async dispatch => {
  dispatch(setGPU(RNGpuInfo.getGlRenderer()));
  await dispatch(fetchPermisions());
  await dispatch(fetchInitialSettings());
  await dispatch(fetchDistribution());
  dispatch(setInitial({ initial: true }));
};
