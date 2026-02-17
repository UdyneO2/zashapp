package com.udyneos.zashboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.udyneos.zashboard.R

class ControlFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get views
        val statusIndicator = view.findViewById<View>(R.id.status_indicator)
        val statusText = view.findViewById<TextView>(R.id.status_text)
        val statusDetail = view.findViewById<TextView>(R.id.status_detail)
        val progressBar = view.findViewById<View>(R.id.progress_bar)
        val switchConnection = view.findViewById<SwitchCompat>(R.id.switch_connection)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group)
        val chipLocalhost = view.findViewById<Chip>(R.id.chip_localhost)
        val chipAdd = view.findViewById<Chip>(R.id.chip_add)
        val etHostname = view.findViewById<TextInputEditText>(R.id.et_hostname)
        val etPort = view.findViewById<TextInputEditText>(R.id.et_port)
        val etSecret = view.findViewById<TextInputEditText>(R.id.et_secret)
        val btnOpenBrowser = view.findViewById<MaterialButton>(R.id.btn_open_browser)
        
        // Register with Activity
        (activity as? MainActivity)?.registerControlViews(
            statusIndicator,
            statusText,
            statusDetail,
            progressBar,
            switchConnection,
            chipGroup,
            chipLocalhost,
            chipAdd,
            etHostname,
            etPort,
            etSecret,
            btnOpenBrowser
        )
    }
}
