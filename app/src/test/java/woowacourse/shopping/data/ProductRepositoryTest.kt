package woowacourse.shopping.data

import com.example.domain.datasource.productsDatasource
import com.example.domain.model.product.Product
import org.junit.Test
import woowacourse.shopping.data.cache.ProductCacheImpl
import woowacourse.shopping.data.datasource.remote.product.ProductMockWebService
import woowacourse.shopping.data.repository.product.ProductRemoteMockRepositoryImpl
import java.util.concurrent.CountDownLatch

internal class ProductRepositoryTest {

    private val productMockWebService: ProductMockWebService = ProductMockWebService()

    @Test
    fun `처음 상품 목록을 가져온다`() {
        val productRemoteRepository = ProductRemoteMockRepositoryImpl(
            productMockWebService,
            ProductCacheImpl.apply { clear() }
        )

        val latch = CountDownLatch(1)
        var actual = emptyList<Product>()
        productRemoteRepository.getProducts(
            page = 1,
            onSuccess = {
                actual = it
                latch.countDown()
            },
            onFailure = {}
        )
        latch.await()

        val expected = productsDatasource.take(20)
        assert(actual == expected)
    }
}
