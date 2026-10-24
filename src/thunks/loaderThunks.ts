import AsyncStorage from '@react-native-async-storage/async-storage';
import { setSuccessDownload } from '../actions/loaderActions';
import { AppThunk } from '../store/store';
export const compareFileRecursion = (): AppThunk => async dispatch => { await AsyncStorage.setItem('isSuccessDownload','true'); dispatch(setSuccessDownload({isSuccessDownload:true})); };
export const fetchStartDownload = (): AppThunk => async dispatch => { await AsyncStorage.setItem('isSuccessDownload','true'); dispatch(setSuccessDownload({isSuccessDownload:true})); };
export const nameFileRecursion = (): AppThunk => async () => false;
export const fetchIsDownloadSuccess = (): AppThunk => async dispatch => { await AsyncStorage.setItem('isSuccessDownload','true'); dispatch(setSuccessDownload({isSuccessDownload:true})); };
