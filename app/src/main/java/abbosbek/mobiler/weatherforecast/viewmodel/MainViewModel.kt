package abbosbek.mobiler.weatherforecast.viewmodel

import abbosbek.mobiler.weatherforecast.model.WeatherModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val liveDataCurrent = MutableLiveData<WeatherModel>()
    val liveDataList = MutableLiveData<List<WeatherModel>>()


}