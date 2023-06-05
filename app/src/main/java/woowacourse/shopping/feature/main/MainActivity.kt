package woowacourse.shopping.feature.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import woowacourse.shopping.R
import woowacourse.shopping.data.PointRemoteRepositoryImpl
import woowacourse.shopping.data.RecentProductRepositoryImpl
import woowacourse.shopping.data.TokenSharedPreference
import woowacourse.shopping.data.cart.CartCache
import woowacourse.shopping.data.cart.CartRemoteRepositoryImpl
import woowacourse.shopping.data.product.ProductCacheImpl
import woowacourse.shopping.data.product.ProductRemoteRepositoryImpl
import woowacourse.shopping.data.service.CartRemoteService
import woowacourse.shopping.data.service.PointRemoteService
import woowacourse.shopping.data.service.ProductRemoteService
import woowacourse.shopping.data.service.ServerInfo
import woowacourse.shopping.data.sql.recent.RecentDao
import woowacourse.shopping.databinding.ActivityMainBinding
import woowacourse.shopping.databinding.DialogCheckPointBinding
import woowacourse.shopping.feature.cart.CartActivity
import woowacourse.shopping.feature.detail.DetailActivity
import woowacourse.shopping.feature.main.load.LoadAdapter
import woowacourse.shopping.feature.main.product.MainProductAdapter
import woowacourse.shopping.feature.main.product.MainProductClickListener
import woowacourse.shopping.feature.main.recent.RecentAdapter
import woowacourse.shopping.feature.main.recent.RecentWrapperAdapter
import woowacourse.shopping.feature.orderHistory.OrderHistoryActivity
import woowacourse.shopping.model.ProductUiModel
import woowacourse.shopping.model.RecentProductUiModel

class MainActivity : AppCompatActivity(), MainContract.View {
    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: MainContract.Presenter
    private lateinit var mainProductAdapter: MainProductAdapter
    private lateinit var recentAdapter: RecentAdapter
    private lateinit var recentWrapperAdapter: RecentWrapperAdapter
    private lateinit var loadAdapter: LoadAdapter
    private lateinit var cartProductCountTv: TextView

    private val concatAdapter: ConcatAdapter by lazy {
        val config = ConcatAdapter.Config.Builder().apply {
            setIsolateViewTypes(false)
        }.build()
        ConcatAdapter(config, recentWrapperAdapter, mainProductAdapter, loadAdapter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initAdapters()
        initLayoutManager()
        initPresenter()

        presenter.loadProducts()
    }

    private fun initAdapters() {
        mainProductAdapter = MainProductAdapter(
            listOf(),
            object : MainProductClickListener {
                override fun onPlusClick(product: ProductUiModel, previousCount: Int) {
                    presenter.increaseCartProduct(product, previousCount)
                }

                override fun onMinusClick(product: ProductUiModel, previousCount: Int) {
                    presenter.decreaseCartProduct(product, previousCount)
                }

                override fun onProductClick(product: ProductUiModel) {
                    presenter.moveToDetail(product)
                }
            }
        )
        recentAdapter = RecentAdapter(listOf()) { recentProduct ->
            presenter.moveToDetail(recentProduct.productUiModel)
        }
        recentWrapperAdapter = RecentWrapperAdapter(recentAdapter)
        loadAdapter = LoadAdapter {
            presenter.loadProducts()
        }
        binding.productRv.adapter = concatAdapter
    }

    private fun initLayoutManager() {
        val layoutManager = GridLayoutManager(this, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (concatAdapter.getItemViewType(position)) {
                    RecentWrapperAdapter.VIEW_TYPE, LoadAdapter.VIEW_TYPE -> 2
                    MainProductAdapter.VIEW_TYPE -> 1
                    else -> 2
                }
            }
        }
        binding.productRv.layoutManager = layoutManager
    }

    private fun initPresenter() {
        val token = TokenSharedPreference.getInstance(this).getToken("") ?: ""
        presenter = MainPresenter(
            this,
            ProductRemoteRepositoryImpl(ProductRemoteService(), ProductCacheImpl),
            RecentProductRepositoryImpl(RecentDao(this, ServerInfo.serverName)),
            CartRemoteRepositoryImpl(CartRemoteService(token), CartCache),
            PointRemoteRepositoryImpl(PointRemoteService(token))
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.updateProducts()
    }

    override fun showCartScreen() {
        startActivity(CartActivity.getIntent(this))
    }

    override fun showProductDetailScreenByProduct(
        product: ProductUiModel,
        recentProduct: ProductUiModel?
    ) {
        startActivity(DetailActivity.getIntent(this, product, recentProduct))
    }

    override fun addProducts(products: List<ProductUiModel>) {
        runOnUiThread {
            binding.mainSkeleton.visibility = View.GONE
            binding.productRv.visibility = View.VISIBLE
            mainProductAdapter.addItems(products)
            presenter.loadRecent()
        }
    }

    override fun updateRecent(recent: List<RecentProductUiModel>) {
        recentAdapter.setItems(recent)
    }

    override fun showProductDetailScreenByRecent(recentProduct: RecentProductUiModel) {
        startActivity(DetailActivity.getIntent(this, recentProduct.productUiModel))
    }

    override fun updateCartProductCount(count: Int) {
        if (::cartProductCountTv.isInitialized) {
            if (count == 0) cartProductCountTv.visibility = View.GONE
            else cartProductCountTv.visibility = View.VISIBLE
            cartProductCountTv.text = count.toString()
        }
    }

    override fun updateProductsCount(products: List<ProductUiModel>) {
        mainProductAdapter.updateItems(products)
    }

    override fun updateProductCount(product: ProductUiModel) {
        mainProductAdapter.updateItem(product)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        menu?.findItem(R.id.cart_action)?.actionView?.let { view ->
            view.setOnClickListener { presenter.moveToCart() }
            view.findViewById<TextView>(R.id.cart_count_tv)?.let { cartProductCountTv = it }
        }
        presenter.setCartProductCount()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.order_history -> navigateToOrderHistory()
            R.id.check_point -> presenter.loadPointInfo()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun navigateToOrderHistory() {
        startActivity(
            Intent(
                this,
                OrderHistoryActivity::class.java,
            ),
        )
    }

    override fun createCheckPointDialog(havePoint: Int, expirePoint: Int) {
        val binding = DialogCheckPointBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this).apply {
            setView(binding.root)
            binding.tvHavePoint.text = getString(R.string.point_format, havePoint)
            binding.tvExpirePoint.text = getString(R.string.point_format, expirePoint)
        }.create().show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        recentWrapperAdapter.onSaveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        recentWrapperAdapter.onRestoreState(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            presenter.refresh()
        }
    }
}
