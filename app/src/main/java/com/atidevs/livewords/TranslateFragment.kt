package com.atidevs.livewords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.atidevs.livewords.databinding.FragmentTranslateBinding
import com.google.common.util.concurrent.ListenableFuture

class TranslateFragment() : Fragment() {

    private var _binding: FragmentTranslateBinding? = null
    private val binding get() = _binding!!

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun handlePermission(isGranted: Boolean) {
        if (isGranted) {
            binding.previewContainer.visibility = View.VISIBLE
            binding.errorContainer.visibility = View.GONE

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            }, ContextCompat.getMainExecutor(requireContext()))
        } else {
            binding.previewContainer.visibility = View.GONE
            binding.errorContainer.visibility = View.VISIBLE
            binding.enablePermissionAction.setOnClickListener {
                (requireActivity() as? MainActivity)?.launchPermissionDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                CameraSelector.LENS_FACING_BACK
            ).build()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }
}