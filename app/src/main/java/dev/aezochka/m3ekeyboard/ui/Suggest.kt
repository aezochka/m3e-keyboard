package dev.aezochka.m3ekeyboard.ui

object Suggest {

    private val en: List<String> = """
    the be to of and a in that have it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most is are was were has had did been may might must should shall cannot hello hi hey thanks thank please sorry ok okay maybe sure welcome congrats love miss talk later soon tonight tomorrow yesterday weekend free busy tired coffee food water lunch dinner breakfast sleep morning evening night phone number message text send call answer problem solution idea thing stuff funny nice cool great awesome weird random
    file folder email account password user name address street city state country zip website internet computer keyboard screen mouse charger battery headphones wifi data update download upload install open close save delete copy paste undo redo settings profile dashboard report project task list note calendar reminder alarm meeting schedule deadline budget invoice payment price cost order shipping delivery tracking return refund support help service customer manager team leader member company office school class teacher student homework exam test quiz score result
    went gone came made took got gave knew thought saw wanted used found told asked worked seemed felt left kept began brought taught bought paid ran met heard played wrote read ate drank slept drove walked jumped finished continued waited watching looking going doing making taking getting giving knowing saying thinking
    big small little large huge tiny short tall long wide narrow old new young early late fast slow cheap expensive easy hard difficult simple interesting boring important free empty full open closed hot cold warm cool wet dry clean dirty light dark happy sad angry glad calm afraid sure ready done fine okay better best worse worst more less most least very really quite pretty such enough almost nearly completely totally probably definitely absolutely maybe perhaps
    here there everywhere somewhere nowhere where why who whom whose which while during before after between through under above below near far inside outside together apart alone again once twice always never often sometimes usually rarely everyday
    water fire earth wind rain snow sun moon star sky sea lake river mountain field forest tree flower grass road path bridge house home door window room kitchen bathroom bedroom table chair bed lamp book pen paper key money wallet bag box gift game sport ball team game win lose play score goal song music movie show story news fact truth lie dream plan hope wish need want try fail succeed start stop pause continue finish return leave arrive depart travel trip journey ticket hotel airport station train bus car bike walk drive ride fly
    """.trim().split(Regex("\\s+"))

    private val ru: List<String> = """
    и в во не что он на я с со как а то все она так его но да ты к у же вы за бы по только ее мне было вот от меня еще нет о из ему теперь когда даже ну вдруг ли если уже или ни быть был него до вас нибудь опять уж вам сказал ведь там потом себя ничего ей может они тут где есть надо ней для мы тебя их чем была сам чтоб без будто человек чего раз тоже себе под будет ж тогда кто этот говорил того потому этого какой совсем ним здесь одном почти мой тем чтобы нее кажется сейчас были куда зачем всех можно при об хорошо этой перед иногда лучше чуть том нельзя такой им более всегда конечно
    привет пока спасибо пожалуйста извини прости ладно давай пошли погнали окей угу ага стоп вали иди сюда туда домой
    работа деньги время день ночь утро вечер неделя месяц год час минута секунда сегодня завтра вчера сейчас потом раньше позже всегда никогда может можно нужно надо хочу могу знаю вижу думаю говорю понимаю слушаю люблю скучаю звоню пишу читаю бегу иду еду жду приду приеду встретимся увидимся договорились отлично супер класс круто здорово ужасно странно нормально
    телефон номер адрес дом квартира улица город страна мир вода еда кофе чай завтрак обед ужин кровать машина дорога билет поезд самолет вокзал аэропорт паспорт сумка кошелек ключи зарядка наушники ноутбук компьютер экран мышь пароль почта файл папка сайт интернет магазин заказ доставка оплата возврат поддержка помощь услуга счет скидка цена стоимость товар вещь
    большой маленький хороший плохой новый старый красивый вкусный горячий холодный быстрый медленный дешевый дорогой легкий сложный интересный скучный важный свободный занятой усталый смешной грустный веселый добрый злой умный глупый белый черный красный синий зеленый желтый
    мам пап брат сестра друг подруга парень девушка жена муж сын дочь ребенок дети семья родители бабушка дедушка
    любить ненавидеть видеть слышать знать думать говорить сказать спросить ответить понять сделать взять дать помочь найти купить продать заплатить начать кончить открыть закрыть включить выключить позвонить написать отправить получить посмотреть послушать подождать успеть опоздать прийти уйти войти выйти сесть встать лечь спать проснуться есть пить гулять играть работать отдыхать учиться учить экзамен урок задание оценка ответ вопрос тема текст слово буква книга страница
    """.trim().split(Regex("\\s+"))

    private val enSet = en
    private val ruSet = ru

    fun suggest(prefix: String, layout: String): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val dict = if (layout == "ru") ruSet else enSet
        val p = prefix.lowercase()
        val matches = dict.filter { it.length > p.length && it.startsWith(p) }.take(3)
        if (matches.isEmpty()) return emptyList()
        return if (prefix[0].isUpperCase()) {
            matches.map { it.replaceFirstChar { c -> c.uppercase() } }
        } else {
            matches
        }
    }
}
