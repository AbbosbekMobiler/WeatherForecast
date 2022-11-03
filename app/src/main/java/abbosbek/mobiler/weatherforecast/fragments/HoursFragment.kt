package abbosbek.mobiler.weatherforecast.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import abbosbek.mobiler.weatherforecast.R
import abbosbek.mobiler.weatherforecast.adapter.WeatherAdapter
import abbosbek.mobiler.weatherforecast.databinding.FragmentHoursBinding
import abbosbek.mobiler.weatherforecast.model.WeatherModel
import abbosbek.mobiler.weatherforecast.viewmodel.MainViewModel
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONArray
import org.json.JSONObject

class HoursFragment : Fragment() {

    private lateinit var binding: FragmentHoursBinding

    private lateinit var adapter: WeatherAdapter

    private val model : MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHoursBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()

        model.liveDataCurrent.observe(viewLifecycleOwner){
            adapter.submitList(getHoursList(it))
        }
    }

    private fun initRcView() = with(binding){
        rcView.layoutManager = LinearLayoutManager(activity)
        adapter = WeatherAdapter(null)
        rcView.adapter = adapter

        val list = listOf(
            WeatherModel("","12:00","Sunny","","25°C","","",""),
            WeatherModel("","13:00","Sunny","","20°C","","",""),
            WeatherModel("","14:00","Cloudy","","18°C","","",""),
            WeatherModel("","15:00","Cloudy","","15°C","","",""),
            WeatherModel("","16:00","Rainy","","10°C","","",""),
        )

    }

    private fun getHoursList(wItem : WeatherModel) : List<WeatherModel>{
        val hoursArray = JSONArray(wItem.hours)
        val list = ArrayList<WeatherModel>()

        for (i in 0 until hoursArray.length()){
            val item = WeatherModel(
                wItem.city,
                (hoursArray[i] as JSONObject).getString("time"),
                (hoursArray[i] as JSONObject).getJSONObject("condition")
                    .getString("text"),
                (hoursArray[i] as JSONObject).getString("temp_c"),
                "",
                "",
                (hoursArray[i] as JSONObject).getJSONObject("condition")
                    .getString("icon"),
                ""
            )

            list.add(item)
        }
        return list
    }

    companion object {

        @JvmStatic
        fun newInstance() = HoursFragment()
    }
}