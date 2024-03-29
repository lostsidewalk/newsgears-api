package com.lostsidewalk.buffy.app.utils;

import static org.apache.commons.lang3.RandomUtils.nextInt;


public class WordUtils {

    enum WORD_SET_1 {
        approve,
        provide,
        utopian,
        inquisitive,
        baby,
        familiar,
        moor,
        capable,
        corn,
        spray,
        parcel,
        pinch,
        condition,
        selection,
        health,
        grey,
        grass,
        mice,
        quizzical,
        diligent,
        icy,
        cows,
        wool,
        shirt,
        toothbrush,
        evasive,
        attract,
        concentrate,
        swing,
        daily,
        quick,
        square,
        field,
        pan,
        curved,
        teenytiny,
        probable,
        tip,
        rambunctious,
        responsible,
        opposite,
        sort,
        oceanic,
    }

    enum WORD_SET_2 {
        enchanting,
        lavish,
        enormous,
        relation,
        functional,
        confused,
        name,
        trail,
        design,
        rhetorical,
        voice,
        yawn,
        grubby,
        hateful,
        preserve,
        nest,
        jagged,
        mix,
        seed,
        heartbreaking,
        veil,
        kaput,
        kindhearted,
        pastoral,
        basketball,
        condemned,
        drop,
        romantic,
        stream,
        sin,
        manage,
        stretch,
        itchy,
        periodic,
        lunch,
        yell,
        connect,
        bump,
        blushing,
        futuristic,
        repulsive,
        beneficial,
        stop,
        yak,
        modern,
        frightened,
        river,
        wire,
        tangy,
        acceptable,
        lick,
        placid,
        bored,
        warm,
        sniff,
        marry,
        sleep,
        miniature,
        roasted,
        blueeyed,
        sore,
        spot,
        wrestle,
        unhealthy,
    }

    enum WORD_SET_3 {
        receipt,
        optimal,
        thrill,
        bumpy,
        halting,
        educated,
        punch,
        command,
        medical,
        extend,
        cherries,
        type,
        hunt,
        observant,
        overrated,
        perpetual,
        glass,
        delicious,
        addicted,
        purring,
        head,
        busy,
        faulty,
        yard,
        stroke,
        bee,
        clumsy,
        fresh,
        seemly,
        dull,
        scatter,
        ink,
        mom,
        attend,
        whip,
        arrange,
        dazzling,
        cap,
        cave,
        thought,
        easy,
        approval,
        list,
        escape,
        absorbed,
        general,
        ice,
        shake,
        uppity,
        hellish,
        play,
        difficult,
        calculate,
        report,
        useful,
        first,
        bell,
        key,
        teaching,
        demonic,
        successful,
        value,
        abstracted,
        pass,
    }

    private static final String THREE_WORD_TEMPLATE = "%s-%s-%s";

    public static String randomWords() {
        int firstIdx = nextInt(0, WORD_SET_1.values().length);
        int secondIdx = nextInt(0, WORD_SET_2.values().length);
        int thirdIdx = nextInt(0, WORD_SET_3.values().length);
        return String.format(THREE_WORD_TEMPLATE,
                WORD_SET_1.values()[firstIdx],
                WORD_SET_2.values()[secondIdx],
                WORD_SET_3.values()[thirdIdx]
        );
    }
}
