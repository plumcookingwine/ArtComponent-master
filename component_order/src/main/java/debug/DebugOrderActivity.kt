package debug

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.kangf.art.order.OrderActivity
import com.kangf.art.order.R
import org.jetbrains.anko.find
import org.jetbrains.anko.intentFor

class DebugOrderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_debug_order)


        find<TextView>(R.id.tvDebug).setOnClickListener {
            startActivity(intentFor<OrderActivity>())
        }

    }


}