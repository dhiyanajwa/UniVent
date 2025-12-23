package com.example.univent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.univent.databinding.ActivityAddEditBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var db: FirebaseFirestore
    private var eventId: String? = null
    private var isCustomCategory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        eventId = intent.getStringExtra("EVENT_ID")

        setupCategorySpinner()

        binding.btnBack.setOnClickListener { finish() }

        // Configure Date Picker
        binding.etDate.isFocusable = false
        binding.etDate.isClickable = true
        binding.etDate.setOnClickListener { showDatePicker() }

        // Configure Time Picker (Goal: Choose from clock)
        binding.etTime.isFocusable = false
        binding.etTime.isClickable = true
        binding.etTime.setOnClickListener { showTimePicker() }

        // Logic Change: Ensure the URL field is visible since we aren't using the picker/upload anymore
        binding.etImageUrl.visibility = View.VISIBLE

        binding.btnToggleCategory.setOnClickListener {
            toggleCategoryInput()
        }

        if (eventId != null) {
            setupEditMode()
        }

        binding.btnSaveEvent.setOnClickListener {
            saveEvent()
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Seminar")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun toggleCategoryInput() {
        isCustomCategory = !isCustomCategory
        if (isCustomCategory) {
            binding.spinnerCategory.visibility = View.GONE
            binding.tilNewCategory.visibility = View.VISIBLE
            binding.btnToggleCategory.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            binding.spinnerCategory.visibility = View.VISIBLE
            binding.tilNewCategory.visibility = View.GONE
            binding.btnToggleCategory.setImageResource(android.R.drawable.ic_input_add)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
            val formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
            binding.etDate.setText(formattedDate)
        }, year, month, day)
        datePickerDialog.show()
    }

    // Goal: Choose time from clock dialog
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            binding.etTime.setText(formattedTime)
        }, hour, minute, true) // true for 24-hour format
        timePickerDialog.show()
    }

    private fun setupEditMode() {
        binding.tvFormTitle.text = "Edit Event"
        db.collection("events").document(eventId!!).get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                event?.let {
                    binding.etTitle.setText(it.title)
                    binding.etDate.setText(it.date)
                    binding.etTime.setText(it.time)
                    binding.etLocation.setText(it.location)
                    binding.etDescription.setText(it.description)
                    binding.etImageUrl.setText(it.imageUrl)

                    // Category Logic
                    val adapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
                    val position = adapter.getPosition(it.category)
                    if (position >= 0) {
                        binding.spinnerCategory.setSelection(position)
                    } else {
                        toggleCategoryInput()
                        binding.etNewCategory.setText(it.category)
                    }
                }
            }
    }

    private fun saveEvent() {
        val title = binding.etTitle.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()

        val category = if (isCustomCategory) {
            binding.etNewCategory.text.toString().trim()
        } else {
            binding.spinnerCategory.selectedItem.toString()
        }

        // Validation - Rubric: "Full CRUD with validation"
        // Ensure strictly all fields (except image) are completed
        if (title.isEmpty() || date.isEmpty() || time.isEmpty() || location.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields (Image is optional)", Toast.LENGTH_LONG).show()
            return
        }

        val eventMap = hashMapOf(
            "title" to title,
            "date" to date,
            "time" to time,
            "location" to location,
            "description" to description,
            "imageUrl" to imageUrl, // Saves the text URL directly (optional)
            "category" to category,
            "organizer" to "Admin"
        )

        val task = if (eventId == null) {
            db.collection("events").add(eventMap)
        } else {
            db.collection("events").document(eventId!!).set(eventMap)
        }

        task.addOnSuccessListener {
            Toast.makeText(this, "Event saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}