package woowacourse.shopping.data

import com.example.domain.ProductCache
import com.example.domain.model.Product
import com.example.domain.repository.ProductRepository
import java.lang.Thread.sleep

class ProductRemoteRepositoryImpl(
    private val service: ProductRemoteService,
    private val productCache: ProductCache
) : ProductRepository {
    override fun getProducts(
        page: Int,
        onSuccess: (List<Product>) -> Unit,
        onFailure: () -> Unit
    ) {
        if (productCache.productList.isEmpty()) {
            Thread {
                service.request(
                    onSuccess = {
                        productCache.addProducts(it)
                        sleep(2000)
                        onSuccess(productCache.getSubProducts(1, LOAD_SIZE))
                    },
                    onFailure = {}
                )
            }.start()
        } else {
            onSuccess(productCache.getSubProducts(page, LOAD_SIZE))
        }
    }

    override fun clearCache() {
        productCache.clear()
    }

    companion object {
        private const val LOAD_SIZE = 20
    }
}