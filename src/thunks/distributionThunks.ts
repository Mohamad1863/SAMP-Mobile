import { setDistribution } from '../actions/distributionActions';
import { AppThunk } from '../store/store';
import { localDistribution } from '../config/localDistribution';
export const fetchDistribution = (): AppThunk => async dispatch => { dispatch(setDistribution(localDistribution)); };
