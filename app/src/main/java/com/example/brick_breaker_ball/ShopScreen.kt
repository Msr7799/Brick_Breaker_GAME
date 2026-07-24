package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align

enum class ShopReturnDestination { MAIN_MENU, PAUSED_GAME }

class ShopScreen(game:BrickBreakerGame,private val returnDestination:ShopReturnDestination):ForgeScreen(game){
    private var pending:ShopProduct?=null
    private var message=""
    private var adTime=-1f
    override fun show(){game.pendingRewards.load()?.let{game.setScreen(RewardRevealScreen(game,it,returnDestination))}}
    override fun render(delta:Float){
        if(adTime>0f)adTime=(adTime-delta).coerceAtLeast(0f)
        begin(2);title("BOOSTER SHOP")
        game.assets.smallFont.draw(batch,"TEST STORE - NO REAL CHARGE",0f,1405f,900f,Align.center,false)
        game.assets.smallFont.draw(batch,"MY BOOSTERS: ${game.boosterInventory.total()}",0f,1358f,900f,Align.center,false)
        val daily=game.dailyRewards.currentState(System.currentTimeMillis())
        val cooldown=game.dailyRewards.remainingCooldownMillis(System.currentTimeMillis())
        val freeLabel=when{adTime>0f->"TEST VIDEO  ${kotlin.math.ceil(adTime).toInt()}";adTime==0f->"CLAIM RANDOM BOOSTER";daily.claimedToday>=5->"DAILY COMPLETE";cooldown>0->"NEXT VIDEO IN %02d:%02d".format(cooldown/60000,(cooldown/1000)%60);else->"WATCH TEST VIDEO  ${daily.claimedToday}/5"}
        val free=button(freeLabel,125f,1240f,650f,82f)
        val mystery=listOf(PowerUpType.EXTRA_LIFE,PowerUpType.FIRE_BALL,PowerUpType.MULTI_BALL)
        mystery.forEachIndexed{i,t->batch.color=Color(1f,1f,1f,.48f);batch.draw(game.assets.gameplayAtlas.powerUpIcon(t),292f+i*105f,1155f,68f,68f)};batch.color=Color.WHITE
        game.assets.smallFont.draw(batch,"RANDOM BOOSTER",0f,1140f,900f,Align.center,false)
        val cards=mutableListOf<Pair<Rectangle,ShopProduct>>()
        ShopCatalog.products.forEachIndexed{i,p->val col=i%2;val row=i/2;val x=45f+col*420f;val y=875f-row*285f
            batch.color=Color(.01f,.05f,.12f,.94f);batch.draw(game.assets.ui.findRegion("panel"),x,y,390f,245f);batch.color=Color.WHITE
            game.assets.hudLabelFont.draw(batch,p.title,x+10f,y+220f,370f,Align.center,false)
            val preview=when(p.id){ShopProductId.RANDOM_5->SHOP_ELIGIBLE_TYPES.take(4);ShopProductId.RANDOM_50->SHOP_ELIGIBLE_TYPES.take(6);else->SHOP_ELIGIBLE_TYPES.take(8)}
            val size=if(preview.size>6)36f else 44f;val gap=4f;val total=preview.size*size+(preview.size-1)*gap;val sx=x+(390f-total)/2f
            preview.forEachIndexed{n,t->batch.draw(game.assets.gameplayAtlas.powerUpIcon(t),sx+n*(size+gap),y+130f,size,size)}
            game.assets.smallFont.draw(batch,p.subtitle,x+10f,y+90f,370f,Align.center,false)
            cards+=button("BUY  ${p.displayPrice}",x+35f,y+5f,320f,60f) to p
        }
        if(message.isNotEmpty())game.assets.smallFont.draw(batch,message,40f,390f,820f,Align.center,true)
        val back=button("BACK",250f,160f,400f,82f)
        pending?.let{p->batch.color=Color(.005f,.02f,.06f,.98f);batch.draw(game.assets.ui.findRegion("panel"),90f,470f,720f,520f);batch.color=Color.WHITE
            title("TEST PURCHASE",920f);game.assets.bodyFont.draw(batch,"${p.title}\n${p.displayPrice}\nNO REAL MONEY WILL BE CHARGED",130f,820f,640f,Align.center,true)
            val confirm=button("CONFIRM TEST PURCHASE",150f,585f,600f,90f);val cancel=button("CANCEL",150f,480f,600f,82f);end()
            when{tapped(confirm)->purchase(p);tapped(cancel)->pending=null};return}
        end()
        when{
            tapped(free)->when{adTime==0f->claimReward();game.dailyRewards.canWatch(System.currentTimeMillis())->adTime=5f}
            cards.firstOrNull{tapped(it.first)}!=null->pending=cards.first{tapped(it.first)}.second
            tapped(back)->goBack()
        }
    }
    private fun purchase(product:ShopProduct){pending=null;game.monetization.purchaseGateway.purchase(product){r->when(r){is PurchaseResult.Success->{if(!game.ledger.hasProcessed(r.transactionId)){val grant=BoosterGrantFactory.createGrant(product,r.transactionId);game.boosterInventory.addAll(grant);game.ledger.markProcessed(r.transactionId);message="PURCHASE COMPLETE: +${grant.values.sum()} BOOSTERS"}};PurchaseResult.Cancelled->message="PURCHASE CANCELLED";is PurchaseResult.Failed->message=r.message}}}
    private fun claimReward(){game.monetization.rewardedAdGateway.show{r->if(r is RewardedAdResult.Earned){val type=BoosterGrantFactory.reward(r.rewardToken);if(!game.ledger.hasProcessed(r.rewardToken)){game.boosterInventory.add(type,1);game.dailyRewards.recordReward(System.currentTimeMillis());game.ledger.markProcessed(r.rewardToken)};val pending=PendingReward(r.rewardToken,type,game.boosterInventory.count(type));game.pendingRewards.save(pending);game.setScreen(RewardRevealScreen(game,pending,returnDestination))};adTime=-1f}}
    private fun goBack(){if(returnDestination==ShopReturnDestination.PAUSED_GAME)game.resumePausedGame()else game.openMenu()}
}
