package com.geekymusketeers.medify.mainFragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.geekymusketeers.medify.R
import com.geekymusketeers.medify.databinding.FragmentStatisticsBinding
import com.google.android.gms.location.LocationRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Collections.max
import java.util.Collections.min
import kotlin.collections.ArrayList


class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreference: SharedPreferences
    private val locationRequest = LocationRequest()


    //Current User's data
    private lateinit var userID: String
    private lateinit var stats: String
    private lateinit var db: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        sharedPreference = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        db = FirebaseDatabase.getInstance().reference
        getDataFromSharedPreference()
        val splitParts = stats.split("?")

        setBloodPressure(splitParts[0], splitParts)
        setSugarFasting(splitParts[1], splitParts)
        setSugarPP(splitParts[2], splitParts)
        setCholesterol(splitParts[3], splitParts)

        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val PERMISSIONS =
            setOf(
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class),
                HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(DistanceRecord::class),
                HealthPermission.getWritePermission(DistanceRecord::class)
            )


        val requestPermissionActivityContract =
            PermissionController.createRequestPermissionResultContract()

        val requestPermissions =
            registerForActivityResult(requestPermissionActivityContract) { granted ->
                if (granted.containsAll(PERMISSIONS)) {
                    // Permissions successfully granted
                } else {
                    // Lack of required permissions
                }
            }

        suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
            // For alpha09 and earlier versions, use getGrantedPermissions(PERMISSIONS) instead
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                // Permissions already granted; proceed with inserting or reading data.
            } else {
                requestPermissions.launch(PERMISSIONS)
            }
        }


        val availabilityStatus =
            requireContext().let {
                HealthConnectClient.sdkStatus(
                    it,
                    "com.google.android.apps.healthdata"
                )
            }

        Log.i("availaibyt", availabilityStatus.toString())
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return // early return as there is no viable integration
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // Optionally redirect to package installer to find a provider, for example:
            val uriString =
                "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
            context?.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", context?.packageName)
                }
            )
            return
        }
        val healthConnectClient = HealthConnectClient.getOrCreate(requireContext())


        suspend fun readDailyRecords(client: HealthConnectClient) {
            // 1
            val today = ZonedDateTime.now()
            val startOfDay = today.truncatedTo(ChronoUnit.DAYS)
            val timeRangeFilter = TimeRangeFilter.between(
                startOfDay.toLocalDateTime(),
                today.toLocalDateTime()
            )

            // 2
            val stepsRecordRequest = ReadRecordsRequest(StepsRecord::class, timeRangeFilter)
            val numberOfStepsToday = client.readRecords(stepsRecordRequest)
                .records
                .sumOf { it.count }
           // Log.i("stepsTextView", numberOfStepsToday.toString())
            // val stepsTextView = findViewById<TextView>(R.id.stepsTodayValue)
            // stepsTextView.text = numberOfStepsToday.toString()

            binding.tvCountSteps.text = (numberOfStepsToday/1000).toString()

            // 3
            val caloriesRecordRequest = ReadRecordsRequest(
                TotalCaloriesBurnedRecord::class,
                timeRangeFilter
            )

            val caloriesBurnedToday = client.readRecords(caloriesRecordRequest)
                .records
              //  .sumOf { it.energy.inCalories.[0] }
            if(caloriesBurnedToday.isNotEmpty()){
                binding.tvCountCalories.text = caloriesBurnedToday[0].energy.inCalories.toString()
            }
            //Log.i("caloriesTextView", caloriesBurnedToday.toString())

           // binding.tvCountCalories.text = caloriesBurnedToday.toString()
            // val caloriesTextView = findViewById<TextView>(R.id.caloriesTodayValue)
            // caloriesTextView.text = caloriesBurnedToday.toString()

            val distanceRecordRequest = ReadRecordsRequest(
                DistanceRecord::class,
                timeRangeFilter
            )

            val distanceToday = client.readRecords(distanceRecordRequest)
                .records
            //Log.i("caloriesTextView", caloriesBurnedToday.toString())

            if (distanceToday.isNotEmpty()){
                binding.tvCountDistance.text = distanceToday[1].distance.inMeters.toString()

            }



            val heartRecordRequest = ReadRecordsRequest(
                HeartRateRecord::class,
                timeRangeFilter
            )

            val heartToday = client.readRecords(heartRecordRequest)
                .records
           // Log.i("caloriesTextView", caloriesBurnedToday.toString())

            if(heartToday.isNotEmpty()){
                binding.tvCountHeartRate.text = heartToday[0].samples[0].beatsPerMinute.toString()

            }
        }



        lifecycleScope.launchWhenCreated {
                checkPermissionsAndRun(healthConnectClient)
        }


        binding.btnImportData.setOnClickListener {
            lifecycleScope.launch {
                readDailyRecords(healthConnectClient)
            }
        }




    }

    @SuppressLint("SetTextI18n", "CommitPrefEdits")
    private fun setBloodPressure(Concat: String, splitParts: List<String>) {
        val splitBloodPressure = Concat.split(":")
        var bloodPressureList: ArrayList<Int> = ArrayList()
        for (i in 0..4) {
            bloodPressureList.add(Integer.parseInt(splitBloodPressure[i]))
        }
        val bloodPressureMin = min(bloodPressureList)
        val bloodPressureMax = max(bloodPressureList)
        if (bloodPressureMax == bloodPressureMax) {
            binding.bloodPressureRange.text = "Value is constant!"
            binding.bloodPressureRange.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.light_blue
                )
            )
        }
        binding.bloodPressureRange.text = "Min: $bloodPressureMin, Max: $bloodPressureMax"
        binding.bloodPressure.setData(bloodPressureList)

        binding.addBloodPressureData.setOnClickListener {
            addNewData(0, bloodPressureList, splitParts)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSugarFasting(Concat: String, splitParts: List<String>) {
        val splitSugarFasting = Concat.split(":")
        val sugarFastingList: ArrayList<Int> = ArrayList()
        for (i in 0..4) {
            sugarFastingList.add(Integer.parseInt(splitSugarFasting[i]))
        }
        val sugarFastingMin = min(sugarFastingList)
        val sugarFastingMax = max(sugarFastingList)
        binding.sugarFastingRange.text = "Min: $sugarFastingMin, Max: $sugarFastingMax"
        binding.sugarFasting.setData(sugarFastingList)

        binding.addSugarFastingData.setOnClickListener {
            addNewData(1, sugarFastingList, splitParts)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSugarPP(Concat: String, splitParts: List<String>) {
        val splitSugarPP = Concat.split(":")
        val sugarPPList: ArrayList<Int> = ArrayList()
        for (i in 0..4) {
            sugarPPList.add(Integer.parseInt(splitSugarPP[i]))
        }
        val sugarPPMin = min(sugarPPList)
        val sugarPPMax = max(sugarPPList)
        binding.sugarPPRange.text = "Min: $sugarPPMin, Max: $sugarPPMax"
        binding.sugarPP.setData(sugarPPList)

        binding.addSugarPPData.setOnClickListener {
            addNewData(2, sugarPPList, splitParts)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCholesterol(Concat: String, splitParts: List<String>) {
        val splitCholesterol = Concat.split(":")
        val cholesterolList: ArrayList<Int> = ArrayList()
        for (i in 0..4) {
            cholesterolList.add(Integer.parseInt(splitCholesterol[i]))
        }
        val cholesterolMin = min(cholesterolList)
        val cholesterolMax = max(cholesterolList)
        binding.cholesterolRange.text = "Min: $cholesterolMin, Max: $cholesterolMax"
        binding.cholesterol.setData(cholesterolList)

        binding.addCholesterolData.setOnClickListener {
            addNewData(3, cholesterolList, splitParts)
        }
    }

    private fun addNewData(ind: Int, List: ArrayList<Int>, splitParts: List<String>) {
        val builder: AlertDialog.Builder = android.app.AlertDialog.Builder(requireActivity())
        builder.setTitle("Title")

        // Set up the input
        val input = EditText(requireActivity())
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.hint = "Enter new data"
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        // Set up the buttons
        builder.setPositiveButton("Set", DialogInterface.OnClickListener { _, _ ->
            // Here you get get input text from the Edittext
            var newDataValue = input.text.toString().trim()

            val queue: LinkedList<Int> = LinkedList(List)
            queue.poll()
            queue.offer(Integer.parseInt(newDataValue))
            List.clear()
            for (i in 0..4) {
                List.add(queue.poll())
            }

            val editor = sharedPreference.edit()
            val merged = mergedData(ind, List, splitParts)
            db.child("Users").child(userID).child("stats").setValue(merged)
            editor.putString("stats", merged)
            editor.apply()
            val fragmentId = findNavController().currentDestination?.id
            findNavController().popBackStack(fragmentId!!, true)
            findNavController().navigate(fragmentId)
        })
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()

    }

    private fun mergedData(i: Int, List: ArrayList<Int>, splitParts: List<String>): String? {
        val tempString: StringBuilder = StringBuilder()
        for (j in 0..4) {
            tempString.append(List[j]).append(":")
        }
        tempString.setLength(tempString.length - 1)
        val finalStats: StringBuilder = StringBuilder()
        for (j in 0..3) {
            if (j == i) {
                finalStats.append(tempString).append("?")
            } else {
                finalStats.append(splitParts.get(j)).append("?")
            }
        }
        finalStats.setLength(finalStats.length - 1)
        return finalStats.toString()
    }


    override fun onStart() {
        super.onStart()
        Handler().postDelayed({
            getDataFromSharedPreference()
        }, 1000)
    }

    @SuppressLint("SetTextI18n")
    private fun getDataFromSharedPreference() {
        userID = sharedPreference.getString("uid", "Not found").toString()
        stats = sharedPreference.getString("stats", "0:0:6:0:0?0:0:0:0:0?0:0:0:0:0?0:0:0:0:0")
            .toString()
    }

}