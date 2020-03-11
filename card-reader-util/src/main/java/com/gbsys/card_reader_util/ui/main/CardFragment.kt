package com.cardreadergb.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cardreadergb.MainActivity

import com.cardreadergb.R
import com.gbsys.card_reader_util.models.PayCard
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.fragment_card.*
import kotlinx.android.synthetic.main.fragment_card.view.*

/**
 * A simple [Fragment] subclass.
 * Use the [CardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CardFragment : Fragment() {

    companion object {
        fun newInstance() = CardFragment()
    }

    private fun formatExpiration(date: Date?) : String?{
        if (date == null) return null
        val formatter = SimpleDateFormat("MM/yyy", Locale.US)
        return formatter.format(date)
    }

    private fun renderPaycardDetails(view: View, payCard: PayCard) {
        view.app_id.text = payCard.payApp.id
        view.app_label.text = payCard.payApp.label
        view.app_tecnology.text = payCard.payApp.technology
        view.paycard_number.text = payCard.number
        view.paycard_expiration.text = formatExpiration(payCard.expiration)
        view.paycard_owner.text = payCard.owner
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_card, container, false)
        val payCard = arguments?.getParcelable<PayCard>("paycard") ?: return view
        renderPaycardDetails(view, payCard)
        view.back_button.setOnClickListener { (activity as MainActivity).goBack() }
        return view
    }

}
