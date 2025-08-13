package com.example.elderly.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.R
import com.example.elderly.presentation.network.Medicamento

class RecordatorioAdapter(
    private var recordatorios: List<Medicamento>
) : RecyclerView.Adapter<RecordatorioAdapter.RecordatorioViewHolder>() {

    inner class RecordatorioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicina: TextView = itemView.findViewById(R.id.tvMedicina)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvTiempo: TextView = itemView.findViewById(R.id.tvTiempo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordatorioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_layout, parent, false)
        return RecordatorioViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordatorioViewHolder, position: Int) {
        val medicamento = recordatorios[position]
        holder.tvMedicina.text = medicamento.medicina
        holder.tvDescripcion.text = medicamento.descripcion ?: ""
        holder.tvFecha.text = medicamento.fecha
        holder.tvTiempo.text = medicamento.tiempo.toString()
    }

    override fun getItemCount(): Int = recordatorios.size

    fun updateData(newRecordatorios: List<Medicamento>) {
        recordatorios = newRecordatorios
        notifyDataSetChanged()
    }
}
