package abbosbek.mobiler.weatherforecast.fragments

import abbosbek.mobiler.weatherforecast.DialogManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import abbosbek.mobiler.weatherforecast.adapter.VpAdapter
import abbosbek.mobiler.weatherforecast.databinding.FragmentMainBinding
import abbosbek.mobiler.weatherforecast.model.WeatherModel
import abbosbek.mobiler.weatherforecast.utils.Constants
import abbosbek.mobiler.weatherforecast.viewmodel.MainViewModel
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

class MainFragment : Fragment() {

    private lateinit var fLocationClient : FusedLocationProviderClient

    private val fList = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )

    private val tList = listOf(
        "Hours",
        "Days"
    )

    private lateinit var pLauncher : ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding

    private val model : MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentMainBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermission()
        init()
        updateCurrentCard()
    }

    private fun updateCurrentCard() = with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){

            val maxMin = "${it.maxTemp}°C/${it.minTemp}°C"

            tvData.text = it.time
            tvCity.text = it.city
            tvCurrentTemp.text = it.currentTemp.ifEmpty { maxMin }
            tvCondition.text = it.condition
            tvMaxMinTemp.text = if (it.currentTemp.isEmpty()) "" else maxMin
            Picasso.get().load("https:"+it.imageUrl).into(imgWeather)
        }
    }

    private fun permissionListener(){
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity, "Permission $it", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission(){
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun init() = with(binding){

        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val adapter = VpAdapter(activity as FragmentActivity,fList)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout,vp){
                tab,pos -> tab.text = tList[pos]
        }.attach()

        ibSync.setOnClickListener{
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocation()
        }

        ibSearch.setOnClickListener {
            DialogManager.searchByNameDialog(requireContext(),object : DialogManager.Listener{
                override fun onClick(name: String?) {
                    name?.let { it1 -> requestWeatherData(it1) }
                }
            })
        }

    }

    private fun getLocation(){
        val ct = CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ){
            return
        }

        fLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,ct.token)
            .addOnCompleteListener {
                requestWeatherData("${it.result.latitude},${it.result.longitude}")
            }
    }

    private fun checkLocation(){
        if (isLocationEnabled()){
            getLocation()
        }else{
            DialogManager.locationSettingDialog(requireContext(),object : DialogManager.Listener{
                override fun onClick(name : String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }
    }

    private fun isLocationEnabled() : Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                Constants.API_KEY +
                "&q=" +
                city +
                "&days=" +
                "3" +
                "&aqi=no" +
                "&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                    result ->parseWeatherData(result = result)

            },
            {
                    error -> Log.d("MyLog","Error : $error")
            }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result : String){
        val mainObject = JSONObject(result)

        val list = parseDays(mainObject)

        parseCurrentDara(mainObject,list[0])

    }

    private fun parseCurrentDara(mainObject : JSONObject,weatherItem : WeatherModel){
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )

        model.liveDataCurrent.value = item
        Log.d("MyLog","MaxTemp : ${item.maxTemp}")
        Log.d("MyLog","MinTemp : ${item.minTemp}")
        Log.d("MyLog","Hours : ${item.hours}")

    }

    private fun parseDays(mainObject: JSONObject) : List<WeatherModel>{
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")

        val name = mainObject.getJSONObject("location").getString("name")

        for (i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}