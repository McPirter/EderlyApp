package com.example.elderly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.elderly.models.Medicamento
import java.text.SimpleDateFormat
import java.util.*

class RecordatorioAdapter(
    private var recordatorios: List<Medicamento>
) : RecyclerView.Adapter<RecordatorioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMedicamento: TextView = view.findViewById(R.id.tvMedicina)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvTiempo: TextView = view.findViewById(R.id.tvTiempo)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recordatorio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recordatorio = recordatorios[position]

        holder.tvMedicamento.text = recordatorio.medicina
        holder.tvDescripcion.text = recordatorio.descripcion ?: "Sin descripción"
        holder.tvTiempo.text = "${recordatorio.tiempo} min"

        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .parse(recordatorio.fecha)
            holder.tvFecha.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(date)
        } catch (e: Exception) {
            holder.tvFecha.text = "Fecha no disponible"
        }
    }

    override fun getItemCount() = recordatorios.size

    fun updateData(newData: List<Medicamento>) {
        recordatorios = newData
        notifyDataSetChanged()
    }
}