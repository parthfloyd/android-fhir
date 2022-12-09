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
import android.content.Context
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.DatabaseErrorStrategy.RECREATE_AT_OPEN
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.ExternalAnswerValueSetResolver
import com.google.android.fhir.demo.data.FhirPeriodicSyncWorker
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.google.android.fhir.demo.data.FhirSyncWorker
import com.google.android.fhir.sync.Sync
import org.hl7.fhir.r4.model.*
import com.google.android.fhir.sync.remote.HttpLogger
import timber.log.Timber

class FhirApplication : Application(), DataCaptureConfig.Provider {
  // Only initiate the FhirEngine when used for the first time, not when the app is created.
  private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

    private val dataCaptureConfiguration by lazy {
        DataCaptureConfig(
            valueSetResolverExternal =
            object : ExternalAnswerValueSetResolver {
                override suspend fun resolve(uri: String): List<Coding> {
                    return lookupCodesFromDb(uri)
                }
            }
        )
    }

    private suspend fun lookupCodesFromDb(uri: String): List<Coding> {
        val valueSets: List<ValueSet> = FhirEngineProvider.getInstance(this).search {
            filter(
                ValueSet.URL,
                {
                    StringFilterModifier.MATCHES_EXACTLY
                    value = uri
                }
            )
        }

        if(valueSets.isEmpty()) {
            return listOf()
        } else {
            val valueSet = valueSets.get(0)
            val codingList = mutableListOf<Coding>()
            valueSet.compose.include.forEach {
                    includeObj ->
                run {
                    includeObj.concept.forEach { conceptObj ->
                        codingList.add(Coding(includeObj.system, conceptObj.code, conceptObj.display))
                    }

                }
            }
            return codingList
        }
    }

    override fun getDataCaptureConfig(): DataCaptureConfig {
        return dataCaptureConfiguration
    }

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
    FhirEngineProvider.init(
      FhirEngineConfiguration(
        enableEncryptionIfSupported = true,
        RECREATE_AT_OPEN,
        ServerConfiguration(
          "https://fhir.dk.swisstph-mis.ch/matchbox/fhir/",
          httpLogger =
            HttpLogger(
              HttpLogger.Configuration(
                if (BuildConfig.DEBUG) HttpLogger.Level.BODY else HttpLogger.Level.BASIC
              )
            ) { Timber.tag("App-HttpLog").d(it) }
        )
      )
    )
    Sync.oneTimeSync<FhirSyncWorker>(this)
  }

  private fun constructFhirEngine(): FhirEngine {
    return FhirEngineProvider.getInstance(this)
  }

  companion object {
    fun fhirEngine(context: Context) = (context.applicationContext as FhirApplication).fhirEngine
  }
}
