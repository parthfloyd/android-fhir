/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.fhir.datacapture.QuestionnaireFragment
import org.hl7.fhir.r4.model.QuestionnaireResponse

/** A fragment class to show patient registration screen. */
class AddPatientFragment : Fragment(R.layout.add_patient_fragment) {

  private val viewModel: AddPatientViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setUpActionBar()
    setHasOptionsMenu(true)
    updateArguments()
    if (savedInstanceState == null) {
      addQuestionnaireFragment()
    }
    observePatientSaveAction()
    (activity as MainActivity).setDrawerEnabled(false)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.add_patient_fragment_menu, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_add_patient_submit -> {
        onSubmitAction()
        true
      }
      android.R.id.home -> {
        NavHostFragment.findNavController(this).navigateUp()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun setUpActionBar() {
    (requireActivity() as AppCompatActivity).supportActionBar?.apply {
      title = requireContext().getString(R.string.add_patient)
      setDisplayHomeAsUpEnabled(true)
    }
  }

  private fun updateArguments() {
    var resources =  listOf("emcareb.registration.e",//0
                            "emcarea.registration.p",//1
                            "emcare.b7.lti-dangersigns",//2
                            "emcare.b18-21.symptoms.2m.m",//3
                            "emcare.b10-14.symptoms.2m.p",//4
                            "emcare.b18-21.signs.2m.m",//5
                            "emcare.b10-16.signs.2m.p",//6
                            "emcare.b23.classification")//7

    var res = resources[7]
    var questionnairename = String.format("questionnaire-%s.json", res)
    var structuremapname = String.format("structuremap-%s.json", res)
    requireArguments()
      .putString(QUESTIONNAIRE_FILE_PATH_KEY, questionnairename)
    requireArguments()
      .putString(STRUCTUREMAP_FILE_PATH_KEY, structuremapname)
  }

  private fun addQuestionnaireFragment() {
    val fragment = QuestionnaireFragment()
    fragment.arguments =
      bundleOf(QuestionnaireFragment.EXTRA_QUESTIONNAIRE_JSON_STRING to viewModel.questionnaire,
                QuestionnaireFragment.EXTRA_QUESTIONNAIRE_RESPONSE_JSON_STRING to viewModel.questionnaireResponse)
    childFragmentManager.commit {
      add(R.id.add_patient_container, fragment, QUESTIONNAIRE_FRAGMENT_TAG)
    }
  }

  private fun onSubmitAction() {
    val questionnaireFragment =
      childFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment
    savePatient(questionnaireFragment.getQuestionnaireResponse())
  }

  private fun savePatient(questionnaireResponse: QuestionnaireResponse) {
    viewModel.savePatient(questionnaireResponse)
  }

  private fun observePatientSaveAction() {
    viewModel.isPatientSaved.observe(viewLifecycleOwner) {
      if (!it) {
        Toast.makeText(requireContext(), "Inputs are missing.", Toast.LENGTH_SHORT).show()
        return@observe
      }
      Toast.makeText(requireContext(), "Patient is saved.", Toast.LENGTH_SHORT).show()
      NavHostFragment.findNavController(this).navigateUp()
    }
  }

  companion object {

    const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    const val STRUCTUREMAP_FILE_PATH_KEY=  "structuremap-file-path-key"
  }
}
