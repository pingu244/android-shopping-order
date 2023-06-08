package woowacourse.shopping.data.datasource.local.recent

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.domain.datasource.productsDatasource
import com.example.domain.model.Product
import com.example.domain.model.RecentProduct
import woowacourse.shopping.data.model.recentProduct.RecentProductEntity
import java.time.LocalDateTime
import java.time.ZoneOffset

class RecentDao(
    context: Context,
    serverName: String
) : SQLiteOpenHelper(context, DB_NAME + serverName, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(RecentContract.createSQL())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${RecentContract.TABLE_NAME}")
        onCreate(db)
    }

    fun selectAllRecent(): List<Long> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM ${RecentContract.TABLE_NAME} ORDER BY ${RecentContract.TABLE_COLUMN_DATE_TIME} desc LIMIT 10",
            null
        )

        val recentViewedProductIds = mutableListOf<Long>()
        while (cursor.moveToNext()) {
            val data = RecentProductEntity(
                cursor.getLong(cursor.getColumnIndexOrThrow(RecentContract.TABLE_COLUMN_RECENT_PRODUCT_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(RecentContract.TABLE_COLUMN_DATE_TIME))
            )

            recentViewedProductIds.add(data.productId)
        }

        cursor.close()
        return recentViewedProductIds
    }

    fun putRecentProduct(recentProduct: RecentProduct) {
        val findRecentProduct =
            selectAll().find { it.product.id == recentProduct.product.id }

        if (findRecentProduct != null) {
            updateRecentProduct(recentProduct)
        } else {
            insertRecentProduct(recentProduct)
        }
    }

    private fun selectAll(): List<RecentProduct> {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM ${RecentContract.TABLE_NAME}",
            null
        )

        val recentProducts = mutableListOf<RecentProduct>()
        while (cursor.moveToNext()) {
            val data = RecentProductEntity(
                cursor.getLong(cursor.getColumnIndexOrThrow(RecentContract.TABLE_COLUMN_RECENT_PRODUCT_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(RecentContract.TABLE_COLUMN_DATE_TIME))
            )
            val product: Product = productsDatasource.find { it.id == data.productId } ?: continue
            val shownDateTime = LocalDateTime.ofEpochSecond(data.dateTimeMills, 0, ZoneOffset.UTC)
            recentProducts.add(RecentProduct(product, shownDateTime))
        }

        cursor.close()
        return recentProducts
    }

    private fun insertRecentProduct(recentProduct: RecentProduct) {
        val timeSecond = recentProduct.dateTime.toEpochSecond(ZoneOffset.UTC)
        val values = ContentValues().apply {
            put(RecentContract.TABLE_COLUMN_RECENT_PRODUCT_ID, recentProduct.product.id)
            put(RecentContract.TABLE_COLUMN_DATE_TIME, timeSecond)
        }
        writableDatabase.insert(RecentContract.TABLE_NAME, null, values)
    }

    private fun updateRecentProduct(recentProduct: RecentProduct) {
        val timeSecond = recentProduct.dateTime.toEpochSecond(ZoneOffset.UTC)
        val updateSql = "UPDATE ${RecentContract.TABLE_NAME} " +
            "SET ${RecentContract.TABLE_COLUMN_DATE_TIME}=$timeSecond " +
            "WHERE ${RecentContract.TABLE_COLUMN_RECENT_PRODUCT_ID}=${recentProduct.product.id}"
        writableDatabase.execSQL(updateSql)
    }

    companion object {
        private const val DB_NAME = "recent_db_"
        private const val VERSION = 1
    }
}
