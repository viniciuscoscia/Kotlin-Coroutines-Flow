package com.viniciuscoscia.kotlincoroutinesflow._1coroutinebasics

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

object ExercisesCoroutinesBasicsEasyAssignment {
	suspend operator fun invoke() {
		cooSound()
	}

	suspend fun cooSound() {
		repeat(4) {
			delay(1000L.milliseconds)
			Log.d("Vini", "Coo")
		}
	}

	suspend fun cawSound() {
		repeat(4) {
			delay(1000L.milliseconds)
			Log.d("Vini", "Caw")
		}
	}

	suspend fun chirpSound() {
		repeat(4) {
			delay(1000L.milliseconds)
			Log.d("Vini", "Chirp")
		}
	}


}

object ExercisesCoroutinesBasicsHardAssignment {
	const val ARARA_IMAGE_URL = "https://scontent-gru2-1.xx.fbcdn.net/v/t39.30808-6/482006808_643466721607271_4060091289478632165_n.jpg?stp=dst-jpg_tt6&cstp=mx1080x1080&ctp=s1080x1080&_nc_cat=107&ccb=1-7&_nc_sid=127cfc&_nc_ohc=xbLCunnnJ60Q7kNvwEc4xho&_nc_oc=Adrce6Nqk8MJb-dpmfUXfBUgqhatuBW-nProfZK690ykC-cEqpPd2gwq4o0G5irxh44&_nc_zt=23&_nc_ht=scontent-gru2-1.xx&_nc_gid=1L7yEL3erlzarBWlJxZPiA&_nc_ss=7b289&oh=00_AQIWxtcIPMvHYOvh1ZnAjT5Ud7gj8cXoF_v94UPbQq2nIw&oe=6AB4F709"
	const val BEM_TE_VI_IMAGE_URL = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRMWikcNR7y_sQr_eMhB-Sm5FzaSxhlD7XThzIIXyH992_N3cun4kmPiT6H&s=10"
	const val PERIQUITO_IMAGE_URL = "https://cptstatic.s3.amazonaws.com/imagens/enviadas/materias/materia10709/cuidados-especiais-com-o-periquito-australiano-cpt5.jpg"

	sealed class Bird(val name: String, val imageUrl: String) {
		data object Arara: Bird(
			name = "Arara",
			imageUrl = ARARA_IMAGE_URL
		)
		data object BemTeVi: Bird(
			name = "Bem-te-vi",
			imageUrl = BEM_TE_VI_IMAGE_URL,
		)
		data object Periquito: Bird(
			name = "Periquito",
			imageUrl = PERIQUITO_IMAGE_URL,
		)
	}

	@Composable
	operator fun invoke() {
		Column(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			var selectedBird: Bird? by remember { mutableStateOf(null) }

			Button(onClick = {
				selectedBird = Bird.Arara
			}) {
				Text("Arara")
			}

			Button(onClick = {
				selectedBird = Bird.BemTeVi
			}) {
				Text("Bem-te-vi")
			}

			Button(onClick = {
				selectedBird = Bird.Periquito
			}) {
				Text("Periquito")
			}

			when (val bird = selectedBird) {
				Bird.Arara,
				Bird.BemTeVi,
				Bird.Periquito -> {
					BirdImage(
						birdImgUrl = bird.imageUrl,
						birdName = bird.name,
					)
				}
				null -> {}
			}
		}
	}


	@Composable
	fun BirdImage(modifier: Modifier = Modifier, birdImgUrl: String, birdName: String) {
		Column(
			modifier = modifier.fillMaxWidth(),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Spacer(modifier = Modifier.size(32.dp))
			Text(birdName)
			AsyncImage(model = birdImgUrl, contentDescription = null)
		}
	}

}