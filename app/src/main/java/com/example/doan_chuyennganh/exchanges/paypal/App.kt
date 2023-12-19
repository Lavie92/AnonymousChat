package com.example.doan_chuyennganh.exchanges.paypal

import android.app.Application
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment
import com.paypal.checkout.config.SettingsConfig
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.UserAction
import com.example.doan_chuyennganh.BuildConfig
class App: Application() {
    val clientID = "AflsPMky-ZldrpZ1bFQXyDM-zTwCvx22zE3OU0A-9owyigYKFyjjr8tR10QypDR5hwo0bAcee9dU6qGm"
    val returnURL = "com.example.doan_chuyennganh://paypalpay"
    override fun onCreate() {
        super.onCreate()
        val config = CheckoutConfig(
            application = this,
            clientId = "AflsPMky-ZldrpZ1bFQXyDM-zTwCvx22zE3OU0A-9owyigYKFyjjr8tR10QypDR5hwo0bAcee9dU6qGm",
            environment = Environment.SANDBOX,
            returnUrl = "com.example.doanchuyennganh://paypalpay",
            currencyCode = CurrencyCode.USD,
            userAction =  UserAction.PAY_NOW,
            settingsConfig = SettingsConfig(
                loggingEnabled = true
            )
        )
        PayPalCheckout.setConfig(config)
    }
}