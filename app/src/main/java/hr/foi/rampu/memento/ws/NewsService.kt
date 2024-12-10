package hr.foi.rampu.memento.ws

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsService {

    @GET("news.php")
    fun getNews(): Call<NewsResponse>

    @GET("news.php")
    fun getNews(@Query("title") newsTitle: String): Call<NewsItem>
}