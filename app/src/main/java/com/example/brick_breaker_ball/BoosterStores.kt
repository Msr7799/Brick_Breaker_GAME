package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class BoosterInventoryStore(private val prefs:Preferences=Gdx.app.getPreferences("brickbreakerball-booster-inventory-v1")) {
    fun count(type:PowerUpType)=if(type in SHOP_ELIGIBLE_TYPES)prefs.getInteger(type.name,0)else 0
    fun snapshot()=SHOP_ELIGIBLE_TYPES.associateWith(::count)
    fun add(type:PowerUpType,amount:Int){require(type in SHOP_ELIGIBLE_TYPES&&amount>=0);prefs.putInteger(type.name,(count(type).toLong()+amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).flush()}
    fun addAll(grants:Map<PowerUpType,Int>){grants.forEach{(t,n)->require(t in SHOP_ELIGIBLE_TYPES&&n>=0);prefs.putInteger(t.name,(count(t).toLong()+n).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())};prefs.flush()}
    fun consume(type:PowerUpType,amount:Int=1):Boolean{if(amount<=0||type !in SHOP_ELIGIBLE_TYPES||count(type)<amount)return false;prefs.putInteger(type.name,count(type)-amount).flush();return true}
    fun refund(type:PowerUpType,amount:Int=1)=add(type,amount)
    fun clearForTests(){prefs.clear();prefs.flush()}
    fun total()=snapshot().values.sum()
}
class TransactionLedger {
    private val prefs=Gdx.app.getPreferences("brickbreakerball-transactions-v1")
    fun hasProcessed(id:String)=prefs.getBoolean(id,false)
    fun markProcessed(id:String){prefs.putBoolean(id,true).flush()}
}
class DailyRewardStore {
    private val prefs=Gdx.app.getPreferences("brickbreakerball-daily-reward-v1")
    fun currentState(now:Long):DailyRewardState{val key=dayKey(now);if(prefs.getString("day","")!=key){prefs.putString("day",key).putInteger("count",0).putLong("next",0).flush()};return DailyRewardState(prefs.getInteger("count",0),5,prefs.getLong("next",0))}
    fun canWatch(now:Long)=currentState(now).let{it.claimedToday<it.dailyLimit&&now>=it.nextEligibleAtMillis}
    fun recordReward(now:Long){val s=currentState(now);prefs.putInteger("count",(s.claimedToday+1).coerceAtMost(5)).putLong("next",now+60_000).flush()}
    fun remainingCooldownMillis(now:Long)=(currentState(now).nextEligibleAtMillis-now).coerceAtLeast(0)
}
