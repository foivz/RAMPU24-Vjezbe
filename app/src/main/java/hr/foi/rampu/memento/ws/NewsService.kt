package hr.foi.rampu.memento.ws

import retrofit2.Call
import retrofit2.http.GET

interface NewsService {

    @GET("news.php")
    fun getNews(): Call<NewsResponse>
}