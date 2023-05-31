package woowacourse.shopping.feature.detail

import com.example.domain.repository.CartRepository
import woowacourse.shopping.mapper.toDomain
import woowacourse.shopping.model.ProductUiModel

class DetailPresenter(
    private val view: DetailContract.View,
    private val cartRepository: CartRepository,
    product: ProductUiModel
) : DetailContract.Presenter {
    private var _product: ProductUiModel = product
    override val product: ProductUiModel
        get() = _product

    private var _count: Int = 1
    override val count get() = _count

    override fun increaseCount() {
        _count++
    }

    override fun decreaseCount() {
        _count--
    }

    override fun selectCount() {
        view.showSelectCountScreen(product)
    }

    override fun addCart() {
        val cartProducts = cartRepository.getAll()
        val cartProductCount = cartProducts.find { it.product.id == product.id }?.count ?: 0
        cartRepository.addProduct(_product.toDomain(), cartProductCount + _count)
        view.showCartScreen()
    }
}
