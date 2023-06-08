package woowacourse.shopping.mapper

import com.example.domain.model.Price
import com.example.domain.model.product.Product
import woowacourse.shopping.model.ProductUiModel

fun ProductUiModel.toDomain(): Product =
    Product(id, name, imgUrl, Price(price))

fun Product.toPresentation(count: Int = 0): ProductUiModel =
    ProductUiModel(id, name, imgUrl, price.value, count)
