package woowacourse.shopping.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderDetailProductDto(
    val usedPoint: Int,
    val products: List<OrderProductDto>
)
