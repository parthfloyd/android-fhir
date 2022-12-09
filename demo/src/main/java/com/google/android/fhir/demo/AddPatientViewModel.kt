/*
 * Copyright 2022 Google LLC
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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.datacapture.createQuestionnaireResponseItem
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import java.util.UUID
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.utils.StructureMapUtilities

/** ViewModel for patient registration screen {@link AddPatientFragment}. */
class AddPatientViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  val questionnaire: String
    get() = getQuestionnaireJson()
  val isPatientSaved = MutableLiveData<Boolean>()
  val structuremap: String
    get() = getStructureMapJson()

  private val questionnaireResource: Questionnaire
    get() =
      FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().parseResource(questionnaire)
        as Questionnaire
  private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)
  private var questionnaireJson: String? = null
  private var structuremapJson: String? = null
  var questionnaireResponse: String? = null

  /**
   * Saves patient registration questionnaire response into the application database.
   *
   * @param questionnaireResponse patient registration questionnaire response
   */
  fun savePatient(questionnaireResponse: QuestionnaireResponse) {
    viewModelScope.launch {
      val structureMap = FhirContext.forR4().newJsonParser().parseResource(structuremap) as StructureMap
      val entry = ResourceMapper.extract(
        questionnaireResource,
        questionnaireResponse,
        StructureMapExtractionContext(context = getApplication()) { _, _ -> structureMap
        }
      )
      print("The Extracted Bundle:")
      print(FhirContext.forR4().newJsonParser().encodeResourceToString(entry))
    }
  }
  private fun getStructureMapJson(): String {
//    questionnaireJson?.let {
//      return it!!
//    }
    structuremapJson = readFileFromAssets(state[AddPatientFragment.STRUCTUREMAP_FILE_PATH_KEY]!!)
    return structuremapJson!!
  }

  private fun getQuestionnaireJson(): String {
//    questionnaireJson?.let {
//      return it!!
//    }
    val parser = FhirContext.forR4().newJsonParser()
    questionnaireJson = readFileFromAssets(state[AddPatientFragment.QUESTIONNAIRE_FILE_PATH_KEY]!!)
    val questionnaireJsonObject = injectUuid(parser.parseResource(Questionnaire::class.java, questionnaireJson))
    questionnaireJson = parser.encodeResourceToString(questionnaireJsonObject)
    questionnaireResponse = parser.encodeResourceToString(generateQuestionnaireResponseWithPatientIdAndEncounterId(questionnaireJsonObject, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
    return questionnaireJson!!
  }
  private fun injectUuid(questionnaire: Questionnaire) : Questionnaire {
    questionnaire.item.forEach { item ->
      if(!item.initial.isNullOrEmpty()) {
        if(item.initial[0].value.asStringValue() == "uuid()") {
          item.initial =
            mutableListOf(Questionnaire.QuestionnaireItemInitialComponent(StringType(
              UUID.randomUUID().toString())))
        }
      }
    }
    return questionnaire
  }


  private fun generateQuestionnaireResponseWithPatientIdAndEncounterId(questionnaireJson: Questionnaire, patientId: String, encounterId: String) : QuestionnaireResponse {
    //Create empty QR as done in the SDC
    val questionnaireResponse:QuestionnaireResponse = QuestionnaireResponse().apply {
      questionnaire = questionnaireJson.url
    }
    questionnaireJson.item.forEach { it2 ->
      questionnaireResponse.addItem(it2.createQuestionnaireResponseItem())
    }

    //Inject patientId as subject & encounterId as Encounter.
    questionnaireResponse.subject = Reference().apply {
      id = IdType(patientId).id
      type = ResourceType.Patient.name
      identifier = Identifier().apply {
        value = patientId
      }
    }
    questionnaireResponse.encounter = Reference().apply {
      id = encounterId
      type = ResourceType.Encounter.name
      identifier = Identifier().apply {
        value = encounterId
      }
    }

    return questionnaireResponse
  }

  private fun readFileFromAssets(filename: String): String {
    return getApplication<Application>().assets.open(filename).bufferedReader().use {
      it.readText()
    }
  }

  private fun generateUuid(): String {
    return UUID.randomUUID().toString()
  }
}
