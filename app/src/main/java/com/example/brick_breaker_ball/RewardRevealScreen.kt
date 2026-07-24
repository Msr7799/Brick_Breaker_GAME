package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import kotlin.math.pow

data class PendingReward(val token:String,val type:PowerUpType,val owned:Int)
class PendingRewardRevealStore(private val prefs:Preferences=Gdx.app.getPreferences("brickbreakerball-pending-reward-v1")){
    fun save(r:PendingReward){prefs.putString("token",r.token).putString("type",r.type.name).putInteger("owned",r.owned).flush()}
    fun load():PendingReward?{val token=prefs.getString("token","");if(token.isEmpty())return null;return runCatching{PendingReward(token,PowerUpType.valueOf(prefs.getString("type")),prefs.getInteger("owned"))}.getOrNull()}
    fun clear(){prefs.clear();prefs.flush()}
}
class RewardRevealScreen(game:BrickBreakerGame,private val reward:PendingReward,private val destination:ShopReturnDestination):ForgeScreen(game){
    private var elapsed=0f
    override fun render(delta:Float){elapsed+=delta;begin(2);title("YOUR REWARD")
        val final=elapsed>=2.45f||game.progress.settings.reduceMotion
        val shown=if(final)reward.type else {val progress=(elapsed/2.45f).coerceIn(0f,1f);val ease=1f-(1f-progress).pow(3);SHOP_ELIGIBLE_TYPES[((ease*45).toInt()).coerceAtMost(44)%SHOP_ELIGIBLE_TYPES.size]}
        batch.color=if(final)Color(.18f,.13f,.03f,.94f)else Color(.01f,.08f,.14f,.94f);batch.draw(game.assets.ui.findRegion("panel"),285f,745f,330f,330f);batch.color=Color.WHITE
        batch.draw(game.assets.gameplayAtlas.powerUpIcon(shown),335f,795f,230f,230f)
        if(final){val info=PowerUpInfoRepository.info(reward.type);game.assets.pauseTitleFont.draw(batch,"AMAZING!",0f,1270f,900f,Align.center,false);game.assets.bodyFont.draw(batch,"YOU GOT",0f,690f,900f,Align.center,false);game.assets.titleFont.draw(batch,info.fullName,35f,620f,830f,Align.center,false);game.assets.bodyFont.draw(batch,"+1     OWNED: ${reward.owned}",0f,535f,900f,Align.center,false);game.assets.smallFont.draw(batch,info.description,70f,470f,760f,Align.center,true);val ok=button("AWESOME",175f,270f,550f,100f);end();if(tapped(ok)||Gdx.input.isKeyJustPressed(Input.Keys.BACK)){game.pendingRewards.clear();game.setScreen(ShopScreen(game,destination))};return}
        game.assets.smallFont.draw(batch,"SHUFFLING...",0f,660f,900f,Align.center,false);end()
    }
}
