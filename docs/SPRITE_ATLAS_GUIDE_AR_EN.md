# دليل Sprite Atlas — عربي / English

## معلومات الإحداثيات / Coordinate Information

- **العربي:** نقطة الأصل أعلى يسار الصورة. `x` إلى اليمين و`y` إلى الأسفل. الصيغة المناسبة لـ Android هي `Rect(x, y, x + width, y + height)`.
- **English:** The origin is the top-left of the image. `x` moves right and `y` moves down. The Android form is `Rect(x, y, x + width, y + height)`.

> **ملاحظة الطوب / Brick note:** أسماء الطوب تصف الخامة والشكل فقط. اربط الوظيفة بـ `BrickType` و`HP` بشكل منفصل. / Brick names describe texture and appearance only. Keep gameplay behavior in separate `BrickType` and `HP` fields.

## عنصر تعزيز / Power-up

### 1. `powerup_expand_paddle` — تكبير المضرب / Expand Paddle

- **الإحداثيات / Coordinates:** `x=1027, y=20, width=173, height=167, right=1200, bottom=187`
- **الغرض / Purpose:** أيقونة بونص إيجابي يزيد عرض المضرب خطوة واحدة. / Positive pickup icon that increases the paddle width by one size step.
- **العمل / Behavior:** عند التقاطه يكبر المضرب حتى الحد الأقصى المحدد. يجعل صد الكرة أسهل، ولا يُفضّل أن يتراكم بلا حد. / When collected, it expands the paddle up to the configured maximum. It makes returning the ball easier and should not stack without a limit.
- **التنفيذ / Implementation:** حدّث مستوى حجم المضرب، ثم غيّر مستطيل التصادم والرسم مع إبقاء المضرب داخل حدود الشاشة. / Increase the paddle-size level, then update both its collision box and draw rectangle while keeping it inside the screen bounds.

### 2. `powerup_brick_thru` — اختراق الطوب / Brick-Thru

- **الإحداثيات / Coordinates:** `x=487, y=21, width=167, height=167, right=654, bottom=188`
- **الغرض / Purpose:** يحوّل الكرة إلى حالة اختراق تمر داخل الطوبات بدل الارتداد عنها. / Changes the ball into a piercing state that travels through bricks instead of bouncing from each one.
- **العمل / Behavior:** تدمر الكرة الطوبات الواقعة في مسارها. في السلوك الكلاسيكي تستطيع التعامل مع الطوبات الخاصة والقوية أيضًا. / The ball destroys bricks along its path. In the classic behavior it can also deal with strong and special bricks.
- **التنفيذ / Implementation:** لا تعكس سرعة الكرة عند اصطدامها بالطوبة أثناء تفعيل الحالة؛ طبّق الضرر ثم اسمح للكرة بالاستمرار. / Do not reflect the ball velocity on brick contact while active; apply damage and let the ball continue.

### 3. `powerup_extra_life` — حياة إضافية / Extra Life

- **الإحداثيات / Coordinates:** `x=666, y=21, width=167, height=166, right=833, bottom=187`
- **الغرض / Purpose:** يزيد عدد المحاولات أو الأرواح بمقدار واحد. / Adds one spare life or attempt.
- **العمل / Behavior:** لا يغيّر مسار الكرة مباشرة، لكنه يمنح اللاعب فرصة إضافية بعد سقوط جميع الكرات. / It does not directly change the ball, but grants another attempt after all balls are lost.
- **التنفيذ / Implementation:** استخدم حدًا أقصى اختياريًا للأرواح، وشغّل مؤثرًا صوتيًا وبصريًا فور الالتقاط. / Optionally enforce a maximum life count and play immediate visual and sound feedback.

### 4. `powerup_kill_paddle` — تدمير المضرب / Kill Paddle

- **الإحداثيات / Coordinates:** `x=844, y=21, width=169, height=166, right=1013, bottom=187`
- **الغرض / Purpose:** بونص سلبي يدمر المضرب فور التقاطه. / Negative pickup that destroys the paddle immediately when collected.
- **العمل / Behavior:** يُنهي المحاولة الحالية عادةً ويخصم حياة واحدة حتى لو كانت الكرة ما زالت في الملعب. / It normally ends the current attempt and removes one life even if a ball is still in play.
- **التنفيذ / Implementation:** عامله كحدث خسارة حياة، وأوقف التكرار حتى لا تُخصم أكثر من حياة من الالتقاط نفسه. / Handle it as a life-loss event and guard against repeated processing so one pickup cannot remove multiple lives.

## مضرب / Paddle

### 5. `paddle_normal` — المضرب العادي / Normal Paddle

- **الإحداثيات / Coordinates:** `x=28, y=29, width=392, height=82, right=420, bottom=111`
- **الغرض / Purpose:** الرسم الأساسي للمضرب عندما لا يكون الليزر أو الإمساك فعالًا. / Base paddle artwork used when neither laser nor grab mode is active.
- **العمل / Behavior:** يرد الكرة ويغيّر زاوية خروجها حسب موضع الاصطدام على سطحه. / Returns the ball and changes its outgoing angle according to the contact position on its surface.
- **التنفيذ / Implementation:** استخدم مستطيل تصادم أبسط من كامل الشفافية، وغيّر زاوية الكرة اعتمادًا على البعد عن مركز المضرب. / Use a collision rectangle tighter than the transparent image bounds and calculate the bounce angle from the distance to the paddle center.

### 6. `paddle_laser` — مضرب الليزر / Laser Paddle

- **الإحداثيات / Coordinates:** `x=15, y=133, width=418, height=150, right=433, bottom=283`
- **الغرض / Purpose:** الرسم المستخدم للمضرب بعد تفعيل بونص إطلاق النار. / Paddle artwork used after activating the shooting power-up.
- **العمل / Behavior:** يحتفظ بعمل المضرب الطبيعي ويضيف مدفعين يطلقان مقذوفات باتجاه الطوب. / Keeps the normal paddle behavior and adds two guns that fire projectiles toward the bricks.
- **التنفيذ / Implementation:** أنشئ طلقتين قرب الطرفين مع فترة انتظار بين الطلقات وحد أقصى للمقذوفات النشطة. / Spawn two shots near the ends with a firing cooldown and a cap on active projectiles.

## عنصر تعزيز / Power-up

### 7. `powerup_set_off_exploding` — تفجير الطوب المتفجر / Set-Off Exploding

- **الإحداثيات / Coordinates:** `x=487, y=195, width=167, height=161, right=654, bottom=356`
- **الغرض / Purpose:** يفجّر جميع الطوبات المصنفة كطوبات متفجرة في المرحلة. / Detonates every brick marked as explosive on the current board.
- **العمل / Behavior:** يبدأ سلسلة انفجارات قد تكسر الطوبات المجاورة وتسبب تفاعلات متسلسلة. / Starts an explosion chain that may destroy neighboring bricks and trigger additional explosive bricks.
- **التنفيذ / Implementation:** اجمع الطوبات المتفجرة أولًا ثم فعّلها عبر قائمة أحداث لتجنب تعديل قائمة الطوب أثناء المرور عليها. / Collect explosive bricks first, then trigger them through an event queue to avoid mutating the brick list while iterating.

### 8. `powerup_level_warp` — الانتقال للمرحلة التالية / Level Warp

- **الإحداثيات / Coordinates:** `x=665, y=195, width=167, height=161, right=832, bottom=356`
- **الغرض / Purpose:** ينهي المرحلة الحالية وينقل اللاعب إلى اللوحة التالية. / Completes the current board and moves the player to the next level.
- **العمل / Behavior:** يتجاوز الطوبات المتبقية من دون الحاجة إلى تدميرها يدويًا. / Skips the remaining bricks without requiring the player to destroy them manually.
- **التنفيذ / Implementation:** استدعِ مسار إنهاء المرحلة الرسمي حتى تُحفظ النقاط والحالة وتُوقف الفيزياء بأمان. / Call the normal level-complete flow so score and state are saved and physics are stopped safely.

### 9. `powerup_shrink_paddle` — تصغير المضرب / Shrink Paddle

- **الإحداثيات / Coordinates:** `x=1027, y=195, width=173, height=161, right=1200, bottom=356`
- **الغرض / Purpose:** يقلل عرض المضرب خطوة واحدة. / Reduces the paddle width by one size step.
- **العمل / Behavior:** يجعل استقبال الكرة أصعب، لكن لا ينبغي أن يقل عن الحد الأدنى. / Makes returning the ball harder, but should not reduce the paddle below its minimum size.
- **التنفيذ / Implementation:** قلّص مستطيل الرسم والتصادم حول المركز، ثم امنع خروج المضرب خارج حدود الشاشة. / Shrink both drawing and collision rectangles around the center, then clamp the paddle to the screen bounds.

### 10. `powerup_shrink_ball` — تصغير الكرة / Shrink Ball

- **الإحداثيات / Coordinates:** `x=844, y=196, width=169, height=161, right=1013, bottom=357`
- **الغرض / Purpose:** يقلل حجم جميع الكرات النشطة أو الكرة المستهدفة حسب تصميمك. / Reduces the size of all active balls, or the targeted ball depending on your design.
- **العمل / Behavior:** تصبح الكرة أصعب في الرؤية والصد، ويمكنها المرور في فراغات أضيق. / The ball becomes harder to see and return, and can fit through narrower gaps.
- **التنفيذ / Implementation:** غيّر نصف قطر التصادم مع الرسم معًا ولا تغيّر مركز الكرة أثناء التحويل. / Update the collision radius and artwork together without moving the ball center during the transformation.

## مضرب / Paddle

### 11. `paddle_grab` — المضرب الماسك / Grab Paddle

- **الإحداثيات / Coordinates:** `x=8, y=296, width=431, height=145, right=439, bottom=441`
- **الغرض / Purpose:** الرسم المستخدم عندما يكون تأثير إمساك الكرة نشطًا. / Paddle artwork used while the ball-catching effect is active.
- **العمل / Behavior:** تلتصق الكرة بالمضرب عند الاصطدام وتتحرك معه حتى يطلقها اللاعب. / A ball sticks to the paddle on contact and moves with it until the player releases it.
- **التنفيذ / Implementation:** خزّن موضع التصاق نسبيًا إلى مركز المضرب، ثم حدّث موقع الكرة أثناء حركة المضرب. / Store the attachment point relative to the paddle center, then update the ball position as the paddle moves.

## عنصر تعزيز / Power-up

### 12. `powerup_fireball` — الكرة النارية / Fireball

- **الإحداثيات / Coordinates:** `x=487, y=365, width=166, height=162, right=653, bottom=527`
- **الغرض / Purpose:** يحوّل الكرة إلى كرة نارية تسبب انفجارًا عند ضرب الطوب. / Transforms the ball into a fireball that creates an explosion when hitting bricks.
- **العمل / Behavior:** تتضرر الطوبة المصابة والطوبات القريبة منها ضمن نصف قطر الانفجار. / The struck brick and nearby bricks are damaged within the explosion radius.
- **التنفيذ / Implementation:** استخدم فحص خلايا مجاورة أو دائرة انفجار، وامنع معالجة الطوبة نفسها مرتين في الانفجار الواحد. / Use neighboring grid cells or an explosion circle and prevent the same brick from being processed twice by one blast.

### 13. `powerup_zap_bricks` — تحييد الطوب الخاص / Zap Bricks

- **الإحداثيات / Coordinates:** `x=666, y=365, width=166, height=162, right=832, bottom=527`
- **الغرض / Purpose:** يحوّل الطوبات الصعبة أو المخفية إلى حالات أسهل قابلة للكسر. / Neutralizes difficult or hidden bricks into easier breakable states.
- **العمل / Behavior:** في السلوك الكلاسيكي يحوّل غير القابل للكسر، ومتعدد الضربات، والمخفي إلى طوب عادي بضربة واحدة. / In the classic behavior, it converts unbreakable, multi-hit, and hidden bricks into regular one-hit bricks.
- **التنفيذ / Implementation:** مرّ على الطوب النشط وعدّل النوع وHP وحالة الظهور، مع تحديث الشكل فورًا. / Iterate over active bricks and update type, HP, and visibility state, refreshing their artwork immediately.

### 14. `powerup_fast_ball` — تسريع الكرة / Fast Ball

- **الإحداثيات / Coordinates:** `x=844, y=365, width=169, height=162, right=1013, bottom=527`
- **الغرض / Purpose:** يرفع سرعة الكرة إلى مستوى عالٍ أو إلى الحد الأقصى. / Raises the ball speed to a high value or the configured maximum.
- **العمل / Behavior:** يزيد صعوبة التحكم ويسرّع تكسير الطوب. / Increases difficulty while speeding up brick destruction.
- **التنفيذ / Implementation:** حافظ على اتجاه السرعة وغيّر مقدارها فقط، مع حد أقصى يمنع اختراق الأجسام بين الإطارات. / Preserve the velocity direction and change only its magnitude, with a maximum that avoids tunneling through objects.

### 15. `powerup_split_ball` — مضاعفة الكرات / Split Ball

- **الإحداثيات / Coordinates:** `x=1027, y=365, width=173, height=162, right=1200, bottom=527`
- **الغرض / Purpose:** يضاعف عدد الكرات الموجودة في الملعب. / Doubles the number of balls currently in play.
- **العمل / Behavior:** ينشئ نسخة من كل كرة باتجاه مختلف قليلًا، ولا تُخسر الحياة حتى تسقط جميع الكرات. / Creates a copy of each ball with a slightly different direction; a life is not lost until all balls are gone.
- **التنفيذ / Implementation:** انسخ قائمة الكرات قبل الإضافة وحدد حدًا أقصى لعدد الكرات لتجنب التضخم والأداء السيئ. / Copy the ball list before adding new balls and enforce a maximum ball count to prevent runaway growth and performance issues.

## كرة / Ball

### 16. `ball_fire_small` — الكرة النارية الصغيرة / Small Fire Ball

- **الإحداثيات / Coordinates:** `x=172, y=470, width=54, height=55, right=226, bottom=525`
- **الغرض / Purpose:** رسم كرة بحجم صغير وحالة نارية تسبب انفجارًا. / Ball artwork with a small size and a state that is fiery and creates explosions.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 17. `ball_thru_small` — كرة الاختراق الصغيرة / Small Brick-Thru Ball

- **الإحداثيات / Coordinates:** `x=302, y=472, width=54, height=53, right=356, bottom=525`
- **الغرض / Purpose:** رسم كرة بحجم صغير وحالة اختراق تمر عبر الطوب. / Ball artwork with a small size and a state that is piercing and passes through bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 18. `ball_normal_small` — الكرة العادية الصغيرة / Small Normal Ball

- **الإحداثيات / Coordinates:** `x=51, y=474, width=44, height=44, right=95, bottom=518`
- **الغرض / Purpose:** رسم كرة بحجم صغير وحالة عادية ترتد عن الطوب. / Ball artwork with a small size and a state that is normal and bounces from bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

## مقذوف / Projectile

### 19. `laser_projectile` — مقذوف الليزر / Laser Projectile

- **الإحداثيات / Coordinates:** `x=407, y=474, width=35, height=111, right=442, bottom=585`
- **الغرض / Purpose:** الرسم العمودي للطلقة التي يطلقها مضرب الليزر. / Vertical projectile artwork fired by the laser paddle.
- **العمل / Behavior:** يتحرك إلى الأعلى ويطبق ضررًا على أول طوبة يصيبها، إلا إذا منحتَه خاصية اختراق. / Moves upward and damages the first brick it hits unless piercing behavior is enabled.
- **التنفيذ / Implementation:** استخدم مستطيل تصادم أضيق من الوهج، واحذف المقذوف عند الخروج أو الاصطدام. / Use a hitbox narrower than the glow and remove the projectile when it leaves the screen or hits.

## عنصر تعزيز / Power-up

### 20. `powerup_laser_paddle` — تسليح المضرب بالليزر / Laser Paddle Power-up

- **الإحداثيات / Coordinates:** `x=487, y=536, width=167, height=158, right=654, bottom=694`
- **الغرض / Purpose:** يفعّل حالة مضرب الليزر ويبدّل الرسم إلى paddle_laser. / Activates the laser-paddle state and switches the artwork to paddle_laser.
- **العمل / Behavior:** يسمح بإطلاق مقذوفات من طرفي المضرب لتدمير الطوب من دون الاعتماد على الكرة فقط. / Allows projectiles to be fired from both paddle ends so bricks can be destroyed without relying only on the ball.
- **التنفيذ / Implementation:** أنشئ حالة مؤقتة أو حتى خسارة الحياة، واربط زر الإطلاق بفترة cooldown. / Create a timed state or keep it until life loss, and connect the fire input to a cooldown.

### 21. `powerup_mega_ball` — الكرة العملاقة / Mega Ball

- **الإحداثيات / Coordinates:** `x=1027, y=536, width=174, height=158, right=1201, bottom=694`
- **الغرض / Purpose:** يزيد حجم الكرة درجة واحدة أو يحولها للحجم الكبير. / Increases the ball size by one step or changes it to the large size.
- **العمل / Behavior:** يسهّل صد الكرة ويزيد مساحة الاصطدام، لكنه قد يمنع دخولها إلى الفتحات الضيقة. / Makes the ball easier to return and increases its collision area, but may stop it from entering narrow gaps.
- **التنفيذ / Implementation:** بدّل إلى sprite الكبير الموافق لنوع الكرة وحدّث نصف القطر من دون تغيير المركز. / Switch to the matching large sprite for the current ball type and update the radius without changing its center.

### 22. `powerup_slow_ball` — إبطاء الكرة / Slow Ball

- **الإحداثيات / Coordinates:** `x=665, y=537, width=167, height=157, right=832, bottom=694`
- **الغرض / Purpose:** يخفض سرعة الكرة إلى الحد الأدنى المسموح. / Reduces the ball speed to the configured minimum.
- **العمل / Behavior:** يجعل التحكم أسهل. في التقليد الكلاسيكي قد يعيد الكرة إلى حالتها العادية ثم تبدأ بالتسارع تدريجيًا. / Makes control easier. In the classic behavior it may also restore the normal ball state and then allow gradual acceleration.
- **التنفيذ / Implementation:** طبّق الحد الأدنى على مقدار متجه السرعة، وحدد صراحةً هل يلغي Fireball وBrick-Thru أم لا. / Apply the minimum to the velocity magnitude and explicitly decide whether it cancels Fireball and Brick-Thru.

### 23. `powerup_super_shrink` — التصغير الفائق للمضرب / Super Shrink

- **الإحداثيات / Coordinates:** `x=844, y=537, width=170, height=158, right=1014, bottom=695`
- **الغرض / Purpose:** يصغّر المضرب مباشرةً إلى أصغر حجم. / Immediately reduces the paddle to its minimum size.
- **العمل / Behavior:** بونص سلبي أقوى من Shrink Paddle لأنه لا يكتفي بدرجة واحدة. / A stronger negative pickup than Shrink Paddle because it jumps directly to the minimum size.
- **التنفيذ / Implementation:** عيّن مستوى الحجم إلى MIN ثم أعد تمركز مستطيل المضرب واقطعه داخل حدود الشاشة. / Set the size level to MIN, recenter the paddle rectangle, and clamp it inside the screen bounds.

## كرة / Ball

### 24. `ball_fire_default` — الكرة النارية العادية / Default Fire Ball

- **الإحداثيات / Coordinates:** `x=155, y=540, width=92, height=90, right=247, bottom=630`
- **الغرض / Purpose:** رسم كرة بحجم عادي وحالة نارية تسبب انفجارًا. / Ball artwork with a default size and a state that is fiery and creates explosions.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 25. `ball_thru_default` — كرة الاختراق العادية / Default Brick-Thru Ball

- **الإحداثيات / Coordinates:** `x=286, y=541, width=87, height=88, right=373, bottom=629`
- **الغرض / Purpose:** رسم كرة بحجم عادي وحالة اختراق تمر عبر الطوب. / Ball artwork with a default size and a state that is piercing and passes through bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 26. `ball_normal_default` — الكرة العادية / Default Normal Ball

- **الإحداثيات / Coordinates:** `x=33, y=544, width=80, height=80, right=113, bottom=624`
- **الغرض / Purpose:** رسم كرة بحجم عادي وحالة عادية ترتد عن الطوب. / Ball artwork with a default size and a state that is normal and bounces from bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 27. `ball_normal_large` — الكرة العادية الكبيرة / Large Normal Ball

- **الإحداثيات / Coordinates:** `x=11, y=645, width=123, height=125, right=134, bottom=770`
- **الغرض / Purpose:** رسم كرة بحجم كبير وحالة عادية ترتد عن الطوب. / Ball artwork with a large size and a state that is normal and bounces from bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 28. `ball_thru_large` — كرة الاختراق الكبيرة / Large Brick-Thru Ball

- **الإحداثيات / Coordinates:** `x=281, y=646, width=123, height=124, right=404, bottom=770`
- **الغرض / Purpose:** رسم كرة بحجم كبير وحالة اختراق تمر عبر الطوب. / Ball artwork with a large size and a state that is piercing and passes through bricks.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

### 29. `ball_fire_large` — الكرة النارية الكبيرة / Large Fire Ball

- **الإحداثيات / Coordinates:** `x=146, y=647, width=124, height=126, right=270, bottom=773`
- **الغرض / Purpose:** رسم كرة بحجم كبير وحالة نارية تسبب انفجارًا. / Ball artwork with a large size and a state that is fiery and creates explosions.
- **العمل / Behavior:** هذا Sprite يمثل الحالة البصرية؛ منطق الحركة والتصادم يبقى في كائن الكرة نفسه. / This sprite represents the visual state; movement and collision logic remain in the ball object.
- **التنفيذ / Implementation:** اختر الرسم حسب BallSize وBallEffect، واجعل نصف قطر الفيزياء متوافقًا مع الحجم المرئي. / Choose the artwork from BallSize and BallEffect, and keep the physics radius consistent with the visible size.

## عنصر تعزيز / Power-up

### 30. `powerup_grab_paddle` — تفعيل المضرب الماسك / Grab Paddle Power-up

- **الإحداثيات / Coordinates:** `x=487, y=702, width=166, height=160, right=653, bottom=862`
- **الغرض / Purpose:** يفعّل قدرة إمساك الكرة ويبدّل الرسم إلى paddle_grab. / Activates the ball-catching ability and switches the artwork to paddle_grab.
- **العمل / Behavior:** بعد الاصطدام بالمضرب تبقى الكرة ملتصقة حتى يضغط اللاعب لإطلاقها. / After paddle contact, a ball remains attached until the player presses to release it.
- **التنفيذ / Implementation:** استخدم قائمة attachedBalls عند وجود أكثر من كرة، وحدد زرًا أو لمسة للإطلاق. / Use an attachedBalls collection when multiple balls exist and define a button or touch action for release.

### 31. `powerup_expand_exploding` — توسيع الانفجارات / Expand Exploding

- **الإحداثيات / Coordinates:** `x=666, y=702, width=166, height=160, right=832, bottom=862`
- **الغرض / Purpose:** يوسّع نطاق الطوبات المتفجرة أو يحول الطوبات المجاورة إلى امتداد متفجر. / Expands the influence of explosive bricks or extends their explosive area into adjacent cells.
- **العمل / Behavior:** يجعل كل انفجار يصيب مساحة أكبر، وقد يزيد فرص التفاعل المتسلسل. / Makes each explosion affect a larger area and can increase chain-reaction opportunities.
- **التنفيذ / Implementation:** خزّن مستوى explosionExpansion للمرحلة واستخدمه عند حساب الخلايا المجاورة بدل تعديل الرسومات فقط. / Store an explosionExpansion level for the board and use it when calculating neighboring cells rather than only changing visuals.

### 32. `powerup_falling_bricks` — نزول الطوب / Falling Bricks

- **الإحداثيات / Coordinates:** `x=844, y=702, width=170, height=160, right=1014, bottom=862`
- **الغرض / Purpose:** بونص سلبي يجعل صفوف الطوب تتحرك إلى الأسفل أثناء اللعب. / Negative pickup that causes brick rows to move downward during play.
- **العمل / Behavior:** في السلوك الكلاسيكي تنزل الطوبات خطوة عندما ترتد الكرة عن المضرب، مما يقلل مساحة الأمان. / In the classic behavior, bricks move down one step when the ball bounces from the paddle, reducing the safe play area.
- **التنفيذ / Implementation:** حرّك مواقع الطوبات منطقيًا ضمن الشبكة، وحدد خط خسارة أو حد توقف قبل وصولها إلى المضرب. / Move brick positions logically in the grid and define a loss line or stopping boundary before they reach the paddle.

### 33. `powerup_eight_ball` — ثماني كرات / Eight Ball

- **الإحداثيات / Coordinates:** `x=1027, y=702, width=173, height=160, right=1200, bottom=862`
- **الغرض / Purpose:** يقسم كرة واحدة إلى ثماني كرات تنتشر بزوايا متعددة. / Splits one ball into eight balls launched at different angles.
- **العمل / Behavior:** يزيد معدل تكسير الطوب بصورة كبيرة، لكن يجعل الشاشة مزدحمة. / Greatly increases brick-breaking speed but makes the playfield crowded.
- **التنفيذ / Implementation:** اختر كرة مصدر واحدة وأنشئ ثماني سرعات بزوايا موزعة، مع حد إجمالي للكرات. / Choose one source ball and create eight velocity vectors at distributed angles, while enforcing a total ball limit.

## مؤثر بصري / Visual effect

### 34. `spark_frame_01` — إطار شرارة 01 / Spark Frame 01

- **الإحداثيات / Coordinates:** `x=23, y=795, width=26, height=28, right=49, bottom=823`
- **الغرض / Purpose:** الإطار رقم 1 من حركة شرارة الاصطدام. / Frame 1 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 35. `spark_frame_02` — إطار شرارة 02 / Spark Frame 02

- **الإحداثيات / Coordinates:** `x=77, y=795, width=27, height=28, right=104, bottom=823`
- **الغرض / Purpose:** الإطار رقم 2 من حركة شرارة الاصطدام. / Frame 2 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 36. `spark_frame_03` — إطار شرارة 03 / Spark Frame 03

- **الإحداثيات / Coordinates:** `x=134, y=795, width=25, height=28, right=159, bottom=823`
- **الغرض / Purpose:** الإطار رقم 3 من حركة شرارة الاصطدام. / Frame 3 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 37. `spark_frame_04` — إطار شرارة 04 / Spark Frame 04

- **الإحداثيات / Coordinates:** `x=185, y=795, width=26, height=28, right=211, bottom=823`
- **الغرض / Purpose:** الإطار رقم 4 من حركة شرارة الاصطدام. / Frame 4 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 38. `spark_frame_05` — إطار شرارة 05 / Spark Frame 05

- **الإحداثيات / Coordinates:** `x=236, y=795, width=27, height=28, right=263, bottom=823`
- **الغرض / Purpose:** الإطار رقم 5 من حركة شرارة الاصطدام. / Frame 5 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 39. `spark_frame_06` — إطار شرارة 06 / Spark Frame 06

- **الإحداثيات / Coordinates:** `x=289, y=795, width=26, height=28, right=315, bottom=823`
- **الغرض / Purpose:** الإطار رقم 6 من حركة شرارة الاصطدام. / Frame 6 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 40. `spark_frame_07` — إطار شرارة 07 / Spark Frame 07

- **الإحداثيات / Coordinates:** `x=343, y=795, width=26, height=28, right=369, bottom=823`
- **الغرض / Purpose:** الإطار رقم 7 من حركة شرارة الاصطدام. / Frame 7 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 41. `spark_frame_08` — إطار شرارة 08 / Spark Frame 08

- **الإحداثيات / Coordinates:** `x=395, y=795, width=25, height=28, right=420, bottom=823`
- **الغرض / Purpose:** الإطار رقم 8 من حركة شرارة الاصطدام. / Frame 8 of the impact-spark animation.
- **العمل / Behavior:** يُعرض لفترة قصيرة عند اصطدام الكرة أو الليزر بالطوب أو المضرب. / Displayed briefly when the ball or laser hits a brick or paddle.
- **التنفيذ / Implementation:** اعرض الإطارات من 01 إلى 08 بالتسلسل ثم احذف كائن المؤثر. / Play frames 01 through 08 in sequence, then remove the effect object.

### 42. `grab_energy_field` — مجال طاقة المضرب الماسك / Grab Paddle Energy Field

- **الإحداثيات / Coordinates:** `x=8, y=833, width=433, height=42, right=441, bottom=875`
- **الغرض / Purpose:** طبقة ضوئية توضع فوق أو أمام المضرب الماسك لإظهار أن القدرة نشطة. / Glowing overlay placed above or in front of the grab paddle to show that its ability is active.
- **العمل / Behavior:** لا يملك تصادمًا مستقلًا؛ هو مؤثر بصري مرتبط بالمضرب. / Has no independent collision; it is a visual effect attached to the paddle.
- **التنفيذ / Implementation:** ارسمه بمحاذاة مركز paddle_grab وبنفس مقياس العرض، ويمكن تغيير الشفافية مع الزمن. / Draw it centered on paddle_grab at the same width scale and optionally pulse its opacity.

## خامة طوبة / Brick texture

### 43. `brick_blue_crystal` — كريستال أزرق / Blue Crystal Brick

- **الإحداثيات / Coordinates:** `x=8, y=884, width=169, height=88, right=177, bottom=972`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية لامعة بضربة واحدة. / Recommended behavior: A glossy one-hit normal brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 44. `brick_purple_armor` — درع بنفسجي / Purple Armor Brick

- **الإحداثيات / Coordinates:** `x=185, y=884, width=169, height=85, right=354, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة مدرعة أو متعددة الضربات. / Recommended behavior: Suitable for an armored or multi-hit brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 45. `brick_light_blue_ice` — جليد أزرق فاتح / Light Blue Ice Brick

- **الإحداثيات / Coordinates:** `x=362, y=884, width=168, height=85, right=530, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة هشة أو عادية بضربة واحدة. / Recommended behavior: Suitable for a fragile or one-hit normal brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 46. `brick_purple_crystal` — كريستال بنفسجي / Purple Crystal Brick

- **الإحداثيات / Coordinates:** `x=538, y=884, width=170, height=85, right=708, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية أو طوبة نقاط مميزة. / Recommended behavior: A normal brick or special score brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 47. `brick_cyan_cracked` — سماوي متشقق / Cyan Cracked Brick

- **الإحداثيات / Coordinates:** `x=717, y=884, width=167, height=85, right=884, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة كحالة ضرر أخيرة لطوبة قوية. / Recommended behavior: Suitable as the final damaged state of a strong brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 48. `brick_blue_ice` — جليد أزرق / Blue Ice Brick

- **الإحداثيات / Coordinates:** `x=892, y=884, width=168, height=85, right=1060, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية أو جليدية حسب قوانين لعبتك. / Recommended behavior: A normal or ice-themed brick depending on your rules.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 49. `brick_light_gray_stone` — حجر رمادي فاتح / Light Gray Stone Brick

- **الإحداثيات / Coordinates:** `x=1067, y=884, width=177, height=85, right=1244, bottom=969`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة حجرية قوية تحتاج عدة ضربات. / Recommended behavior: Suitable for a strong stone brick requiring multiple hits.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 50. `brick_green_crystal` — كريستال أخضر / Green Crystal Brick

- **الإحداثيات / Coordinates:** `x=8, y=976, width=168, height=85, right=176, bottom=1061`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية لامعة بضربة واحدة. / Recommended behavior: A glossy one-hit normal brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 51. `brick_gold_honeycomb` — خلية ذهبية / Gold Honeycomb Brick

- **الإحداثيات / Coordinates:** `x=185, y=976, width=169, height=84, right=354, bottom=1060`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة مكافأة أو نقاط أعلى. / Recommended behavior: Suitable for a bonus or higher-score brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 52. `brick_dark_lava` — حمم داكنة / Dark Lava Brick

- **الإحداثيات / Coordinates:** `x=362, y=976, width=168, height=84, right=530, bottom=1060`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة متفجرة أو طوبة خطر. / Recommended behavior: Suitable for an explosive or hazard brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 53. `brick_brown_rock` — صخرة بنية / Brown Rock Brick

- **الإحداثيات / Coordinates:** `x=538, y=976, width=170, height=85, right=708, bottom=1061`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة قوية متعددة الضربات. / Recommended behavior: Suitable for a strong multi-hit brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 54. `brick_orange_lava` — حمم برتقالية / Orange Lava Brick

- **الإحداثيات / Coordinates:** `x=716, y=976, width=168, height=84, right=884, bottom=1060`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة متفجرة واضحة. / Recommended behavior: Suitable for a clearly explosive brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 55. `brick_red_lava` — حمم حمراء / Red Lava Brick

- **الإحداثيات / Coordinates:** `x=892, y=977, width=168, height=84, right=1060, bottom=1061`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة متفجرة شديدة أو مرحلة ضرر ساخنة. / Recommended behavior: Suitable for a strong explosive brick or hot damage state.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 56. `brick_gray_stone` — حجر رمادي / Gray Stone Brick

- **الإحداثيات / Coordinates:** `x=1067, y=977, width=177, height=84, right=1244, bottom=1061`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة قوية أو حجرية متعددة الضربات. / Recommended behavior: Suitable for a strong or multi-hit stone brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 57. `brick_gold_ore` — خام ذهبي / Gold Ore Brick

- **الإحداثيات / Coordinates:** `x=8, y=1070, width=170, height=83, right=178, bottom=1153`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة كنز تمنح نقاطًا إضافية أو بونصًا. / Recommended behavior: Suitable for a treasure brick that grants extra score or a pickup.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 58. `brick_red_masonry` — طوب أحمر / Red Masonry Brick

- **الإحداثيات / Coordinates:** `x=185, y=1070, width=169, height=82, right=354, bottom=1152`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة بناء عادية بضربة واحدة. / Recommended behavior: A standard one-hit masonry brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 59. `brick_light_green_stone` — حجر أخضر فاتح / Light Green Stone Brick

- **الإحداثيات / Coordinates:** `x=359, y=1070, width=178, height=83, right=537, bottom=1153`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة قوية أو مرحلة سليمة قبل التشقق. / Recommended behavior: Suitable for a strong brick or intact state before cracking.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 60. `brick_green_cracked` — أخضر متشقق / Green Cracked Brick

- **الإحداثيات / Coordinates:** `x=538, y=1070, width=170, height=83, right=708, bottom=1153`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة كحالة ضرر لطوبة الحجر الأخضر. / Recommended behavior: Suitable as a damaged state for the green stone brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 61. `brick_green_metal` — معدن أخضر / Green Metal Brick

- **الإحداثيات / Coordinates:** `x=717, y=1070, width=167, height=82, right=884, bottom=1152`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة مدرعة أو شبه غير قابلة للكسر. / Recommended behavior: Suitable for an armored or nearly unbreakable brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 62. `brick_lime_crystal` — كريستال أخضر لامع / Lime Crystal Brick

- **الإحداثيات / Coordinates:** `x=892, y=1070, width=172, height=82, right=1064, bottom=1152`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية أو مكافأة بصرية. / Recommended behavior: A normal brick or visually marked reward brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 63. `brick_dark_masonry` — بناء داكن / Dark Masonry Brick

- **الإحداثيات / Coordinates:** `x=1067, y=1070, width=177, height=83, right=1244, bottom=1153`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة حجرية قوية أو مرحلة متقدمة. / Recommended behavior: Suitable for a strong masonry brick or later-level theme.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 64. `brick_pink_crystal` — كريستال وردي / Pink Crystal Brick

- **الإحداثيات / Coordinates:** `x=8, y=1160, width=168, height=86, right=176, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة عادية أو طوبة نقاط مميزة. / Recommended behavior: A normal brick or special-score brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 65. `brick_dark_metal` — معدن داكن / Dark Metal Brick

- **الإحداثيات / Coordinates:** `x=185, y=1160, width=169, height=87, right=354, bottom=1247`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: أفضل اختيار بصري للطوبة غير القابلة للكسر. / Recommended behavior: Best visual choice for an unbreakable brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 66. `brick_blue_masonry` — طوب أزرق / Blue Masonry Brick

- **الإحداثيات / Coordinates:** `x=362, y=1160, width=168, height=86, right=530, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: طوبة بناء عادية أو قوية قليلًا. / Recommended behavior: A normal masonry brick or slightly stronger variant.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 67. `brick_blue_rock` — صخرة زرقاء / Blue Rock Brick

- **الإحداثيات / Coordinates:** `x=538, y=1160, width=170, height=86, right=708, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة لطوبة قوية متعددة الضربات. / Recommended behavior: Suitable for a strong multi-hit rock brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 68. `brick_light_gray_cracked` — رمادي فاتح متشقق / Light Gray Cracked Brick

- **الإحداثيات / Coordinates:** `x=717, y=1160, width=170, height=86, right=887, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: حالة ضرر متوسطة أو أخيرة لطوبة رمادية قوية. / Recommended behavior: A middle or final damaged state for a strong gray brick.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 69. `brick_dark_gray_cracked` — رمادي داكن متشقق / Dark Gray Cracked Brick

- **الإحداثيات / Coordinates:** `x=892, y=1160, width=172, height=86, right=1064, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: حالة ضرر شديدة قبل تدمير الطوبة. / Recommended behavior: A heavily damaged state before the brick is destroyed.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.

### 70. `brick_hidden_question` — طوبة مخفية بعلامة استفهام / Hidden Question Brick

- **الإحداثيات / Coordinates:** `x=1067, y=1160, width=177, height=86, right=1244, bottom=1246`
- **الغرض / Purpose:** خامة رسومية للطوبة. اللون والشكل لا يفرضان وظيفة ثابتة. / A visual brick texture. Its color and appearance do not impose a fixed gameplay function.
- **العمل / Behavior:** السلوك المقترح: مناسبة كرمز داخل محرر المراحل لطوبة مخفية أو عشوائية؛ داخل اللعب يمكن إخفاء الرسم حتى تُكتشف. / Recommended behavior: Suitable as an editor marker for a hidden or random brick; during gameplay the artwork can remain hidden until revealed.
- **التنفيذ / Implementation:** خزّن textureName منفصلًا عن BrickType وHP، حتى تستطيع استخدام الرسم نفسه لأكثر من قاعدة لعب. / Store textureName separately from BrickType and HP so the same artwork can be reused with different gameplay rules.
