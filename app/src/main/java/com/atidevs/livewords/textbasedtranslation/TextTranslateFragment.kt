package com.atidevs.livewords.textbasedtranslation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.atidevs.livewords.databinding.FragmentTextTranslateBinding

class TextTranslateFragment : Fragment() {

    private var _binding: FragmentTextTranslateBinding? = null
    private val binding get() = _binding!!

    private val textTranslateViewModel: TextTranslateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTextTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}