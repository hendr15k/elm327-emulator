package com.elm327.emulator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.elm327.emulator.R
import com.elm327.emulator.bluetooth.Elm327LocalTest
import com.elm327.emulator.databinding.ItemTestResultBinding

class TestResultAdapter : RecyclerView.Adapter<TestResultAdapter.TestResultViewHolder>() {

    private val results = mutableListOf<Elm327LocalTest.TestResult>()

    fun setResults(newResults: List<Elm327LocalTest.TestResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
        val binding = ItemTestResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TestResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount() = results.size

    class TestResultViewHolder(private val binding: ItemTestResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: Elm327LocalTest.TestResult) {
            binding.testName.text = result.name
            binding.testDetail.text = result.detail

            val colorRes = if (result.passed) R.color.status_green else R.color.status_red
            binding.testIndicator.setBackgroundColor(
                ContextCompat.getColor(itemView.context, colorRes)
            )
        }
    }
}
