package com.poker.holdem.handlogic.card;

import java.io.Serializable;

public class Card implements Comparable<Card>, Serializable {
    private Rank rank;
    private Suit suit;

    public Card (Rank r, Suit s){
        this.rank = r;
        this.suit = s;
    }
    //специальный конструктор для работы с кодами карт

    public Card(int card){
        switch (card%100){
            case 2: this.rank = Rank.TWO; break;
            case 3: this.rank = Rank.THREE; break;
            case 4: this.rank = Rank.FOUR; break;
            case 5: this.rank = Rank.FIVE; break;
            case 6: this.rank = Rank.SIX; break;
            case 7: this.rank = Rank.SEVEN; break;
            case 8: this.rank = Rank.EIGHT; break;
            case 9: this.rank = Rank.NINE; break;
            case 10: this.rank = Rank.TEN; break;
            case 11: this.rank = Rank.JACK; break;
            case 12: this.rank = Rank.QUEEN; break;
            case 13: this.rank = Rank.KING; break;
            case 14: this.rank = Rank.ACE; break;
        }
        switch (card/100){
            case 1: this.suit = Suit.DIAMONDS;break;
            case 2: this.suit = Suit.CLUBS;break;
            case 3: this.suit = Suit.HEARTS;break;
            case 4: this.suit = Suit.SPADES;break;
        }
    }

    public Integer getCode(){
        return this.suit.getSuit_val()*100+this.rank.getRank_val();
    }

    public Rank getRank() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    @Override
    public int compareTo(Card o) {
        final int rankComparison = Integer.compare(this.rank.getRank_val(), o.rank.getRank_val());
        return rankComparison != 0 ? rankComparison : Integer.compare(this.suit.getSuit_val(), o.suit.getSuit_val());
    }
}

