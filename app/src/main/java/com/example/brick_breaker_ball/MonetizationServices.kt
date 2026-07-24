package com.example.brick_breaker_ball

import java.util.UUID

sealed interface PurchaseResult{data class Success(val transactionId:String):PurchaseResult;data object Cancelled:PurchaseResult;data class Failed(val message:String):PurchaseResult}
interface PurchaseGateway{fun purchase(product:ShopProduct,callback:(PurchaseResult)->Unit)}
sealed interface RewardedAdResult{data class Earned(val rewardToken:String):RewardedAdResult;data object ClosedWithoutReward:RewardedAdResult;data class Failed(val message:String):RewardedAdResult}
interface RewardedAdGateway{fun show(callback:(RewardedAdResult)->Unit)}
class FakePurchaseGateway:PurchaseGateway{override fun purchase(product:ShopProduct,callback:(PurchaseResult)->Unit)=callback(PurchaseResult.Success("test-${product.storeId}-${UUID.randomUUID()}"))}
class FakeRewardedAdGateway:RewardedAdGateway{override fun show(callback:(RewardedAdResult)->Unit)=callback(RewardedAdResult.Earned("reward-${UUID.randomUUID()}"))}
data class MonetizationServices(val purchaseGateway:PurchaseGateway,val rewardedAdGateway:RewardedAdGateway){companion object{fun fake()=MonetizationServices(FakePurchaseGateway(),FakeRewardedAdGateway())}}
