package com.bangkit.glowfyapp.view.home.fragments.clinic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bangkit.glowfyapp.BuildConfig.MAPS_API_KEY
import com.bangkit.glowfyapp.R
import com.bangkit.glowfyapp.data.models.ClinicData
import com.bangkit.glowfyapp.databinding.FragmentClinicBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class ClinicFragment : Fragment(), OnMapReadyCallback, ClinicAdapter.ClinicItemClickListener {

    private var _binding: FragmentClinicBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClinicBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Places.initialize(requireContext(), MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        if (!isLocationEnabled()) {
            showLocationAlert()
        } else {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        Log.d("testlokasi", "Current location: $currentLatLng")
                        mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        findNearbyClinics(currentLatLng)
                    }
                }
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (::mMap.isInitialized) {
                mMap.clear()
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            Log.d("testlokasi", "Current location: $currentLatLng")
                            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            findNearbyClinics(currentLatLng)
                        }
                    }
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationAlert() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Enable Location")
        alertDialog.setMessage("Your locations setting is set to 'Off'. Please enable location to use this app")
        alertDialog.setPositiveButton("Location Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        alertDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        alertDialog.show()
    }

    private fun findNearbyClinics(location: LatLng) {
        showLoading(true)
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            showLoading(false)
            binding.swipeRefreshLayout.isRefreshing = false
            if (task.isSuccessful) {
                val response = task.result
                val places = mutableListOf<ClinicData>()
                for (placeLikelihood in response?.placeLikelihoods ?: emptyList()) {
                    val place = placeLikelihood.place
                    if (place.types?.contains(Place.Type.HEALTH) == true || place.types?.contains(
                            Place.Type.BEAUTY_SALON) == true) {
                        place.latLng?.let {
                            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_clinic_marker)
                            val bitmap = drawable?.toBitmap()
                            val icon = bitmap?.let { it1 -> BitmapDescriptorFactory.fromBitmap(it1) }


                            places.add(ClinicData(place.name, place.address, it, bitmap, MarkerOptions()))
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(it)
                                    .title(place.name)
                                    .icon(icon)
                            )
                        }
                    }
                }
                showPlacesInRecyclerView(places)
            } else {
                Toast.makeText(requireContext(), "Failed to find nearby clinics", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPlacesInRecyclerView(places: List<ClinicData>) {
        val clinicRv = binding.clinicRv
        clinicRv.layoutManager = LinearLayoutManager(requireContext())
        val clinicAdapter = ClinicAdapter(places)
        clinicAdapter.setOnItemClickListener(this)
        clinicRv.adapter = clinicAdapter
    }

    override fun onClinicItemClick(clinicData: ClinicData) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clinicData.location, 17f))
    }

    private fun showLoading(onLoading: Boolean){
        if(onLoading){
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}