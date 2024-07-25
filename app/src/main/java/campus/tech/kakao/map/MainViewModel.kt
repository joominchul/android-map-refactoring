package campus.tech.kakao.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import campus.tech.kakao.map.dto.Document
import campus.tech.kakao.map.dto.MapPositionPreferences
import campus.tech.kakao.map.dto.SearchWord
import campus.tech.kakao.map.dto.SearchWordDao
import campus.tech.kakao.map.url.RetrofitData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class MainViewModel @Inject constructor(
	application: Application, private val retrofitData: RetrofitData,
	private val searchWordDao: SearchWordDao, private val mapPosition: MapPositionPreferences
) : AndroidViewModel(application) {

	private val _wordList = MutableLiveData<List<SearchWord>>()
	val wordList: LiveData<List<SearchWord>> get() =  _wordList

	val documentList: LiveData<List<Document>> get() = retrofitData.getDocuments()

	private val _documentClicked = MutableLiveData<Boolean>()
	val documentClicked: LiveData<Boolean> get() = _documentClicked


	fun addWord(document: Document){
		val word = wordFromDocument(document)
		thread {
			deleteWord(word)
			searchWordDao.insert(word)
			loadWord()
		}

	}

	private fun wordFromDocument(document: Document): SearchWord {
		return SearchWord(document.placeName, document.addressName, document.categoryGroupName)
	}
	fun deleteWord(word: SearchWord){
		thread {
			searchWordDao.delete(word.name, word.address, word.type)
			loadWord()
		}.join()
	}

	fun loadWord(){
		thread {
			_wordList.postValue(searchWordDao.getAll())
		}
	}

	fun searchLocalAPI(query: String){
		retrofitData.searchPlace(query)
	}


	private fun setMapInfo(document: Document){
		mapPosition.setMapInfo(document)
	}

	fun getMapInfo():List<String>{
		val latitude = mapPosition.getPreferences(LATITUDE,INIT_LATITUDE)
		val longitude = mapPosition.getPreferences(LONGITUDE,INIT_LONGITUDE)
		val placeName = mapPosition.getPreferences(PLACE_NAME,"")
		val addressName = mapPosition.getPreferences(ADDRESS_NAME,"")
		return listOf(latitude, longitude, placeName, addressName)
	}

	fun placeClicked(document: Document){
		_documentClicked.value = false
		addWord(document)
		setMapInfo(document)
		_documentClicked.value = true
	}

	fun documentClickedDone(){
		_documentClicked.value = false
	}

	companion object{
		const val INIT_LATITUDE = "37.402005"
		const val INIT_LONGITUDE = "127.108621"
		const val LATITUDE = "latitude"
		const val LONGITUDE = "longitude"
		const val PLACE_NAME = "placeName"
		const val ADDRESS_NAME = "addressName"
	}
}