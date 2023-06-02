package woowacourse.shopping.feature.order

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.domain.model.Point
import woowacourse.shopping.R
import woowacourse.shopping.data.CartCache
import woowacourse.shopping.data.CartRemoteRepositoryImpl
import woowacourse.shopping.data.PointRemoteRepositoryImpl
import woowacourse.shopping.data.TokenSharedPreference
import woowacourse.shopping.data.service.CartRemoteService
import woowacourse.shopping.data.service.PointRemoteService
import woowacourse.shopping.databinding.ActivityOrderBinding
import woowacourse.shopping.model.CartProductUiModel

class OrderActivity : AppCompatActivity(), OrderContract.View {

    private lateinit var binding: ActivityOrderBinding
    private lateinit var presenter: OrderContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPresenter()

        presenter.loadProducts()
        presenter.loadPayment()
    }

    private fun initPresenter() {
        val token = TokenSharedPreference.getInstance(this).getToken("") ?: ""
        presenter =
            OrderPresenter(
                this, CartRemoteRepositoryImpl(CartRemoteService(token), CartCache),
                PointRemoteRepositoryImpl(PointRemoteService(token)),
            )
    }

    override fun initAdapter(orderProducts: List<CartProductUiModel>) {
        binding.recyclerviewOrderProducts.adapter = OrderProductAdapter(
            products = orderProducts
        )
    }

    override fun setUpView(point: Point, productsPrice: Int) {
        with(binding) {
            tvProductsPrice.text = getString(R.string.price_format, productsPrice)
            tvHavePoint.text = getString(R.string.holding_point, point.value)
            btnUseAll.setOnClickListener {
                etUsePoint.setText(point.value.toString())
            }
            btnOrder.text = getString(R.string.order_btn, productsPrice)

            btnOrder.setOnClickListener {
//            presenter.orderProducts(productsPrice)
                finish()
            }
        }

        setEditTextState(point, productsPrice)
    }

    private fun setEditTextState(point: Point, productsPrice: Int) {
        binding.etUsePoint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(inputValue: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val inputPoint =
                    if (inputValue.isNullOrBlank()) 0 else inputValue.toString().toInt()
                if (inputPoint > point.value) {
                    Toast.makeText(this@OrderActivity, OVER_POINT_ERROR, Toast.LENGTH_SHORT)
                        .show()
                    binding.etUsePoint.text = null
                    binding.btnOrder.text = getString(R.string.order_btn, productsPrice)
                } else {
                    binding.btnOrder.text =
                        getString(R.string.order_btn, productsPrice - inputPoint)
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
    }

    companion object {
        private const val OVER_POINT_ERROR = "보유한 포인트 값을 초과합니다"
    }
}
