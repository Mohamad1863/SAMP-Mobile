import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import React, { useEffect } from 'react';
import { Text, View } from 'react-native';
import { LoaderContainer } from '../components';
import { useAppDispatch } from '../hooks/useAppDispatch';
import { useAppSelector } from '../hooks/useAppSelector';
import { selectInitial } from '../selectors/appSelectors';
import { styles } from '../styles/LoaderStyle';
import { fetchInitialApp } from '../thunks/appThunks';
type InitiationScreenType = NativeStackScreenProps<any>;
export const InitiationScreen = React.memo(({ navigation }: InitiationScreenType) => {
  const dispatch = useAppDispatch(); const isInitial = useAppSelector(selectInitial);
  useEffect(() => { dispatch(fetchInitialApp()); }, [dispatch]);
  useFocusEffect(React.useCallback(() => { if (isInitial) navigation.replace('Main'); return () => {}; }, [isInitial,navigation]));
  return <LoaderContainer><View style={styles.progress}><Text style={styles.starting}>در حال آماده‌سازی بازی...</Text></View></LoaderContainer>;
});
