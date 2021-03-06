package com.poker.holdem;

import android.content.Context;
import android.widget.Toast;

import com.poker.holdem.constants.Constants;
import com.poker.holdem.logic.GameStatsHolder;
import com.poker.holdem.logic.player.Player;
import com.poker.holdem.view.util.ViewControllerActionCode;
import com.poker.holdem.view.util.ViewControllerTimerConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class Presenter implements GameContract.Presenter {

    private GameContract.View gameView;
    private GameContract.Server serverController;

    private String ROOM_NAME = "";
    private String PLAYER_NAME = "";

    private boolean flagToWaitBeforeGameStarts = false;

    //просто контейнер для кучи значений
    private GameStatsHolder gameStats;

    public Presenter(GameContract.View view, String roomName){
        this.ROOM_NAME = roomName;
        this.PLAYER_NAME = PokerApplicationManager
                .getInstance()
                .getSharedPreferences(
                        Constants.PREFS_NAME
                        ,Context.MODE_PRIVATE
                ).getString(Constants.PLAYER_NAME, "");

        this.serverController = new ServerController(this) ;
        this.gameView = view;
        this.serverController.sendMessageOnServerEnterLobby(this.ROOM_NAME);

        gameStats = new GameStatsHolder();
    }

    //Тут то, что мы получаем от GameViewFragment
    @Override
    public void foldButtonClicked() {
        if(gameStats.getLead().equals(this.PLAYER_NAME)
                && !gameStats.isPlayerPushedButton()
                && gameStats.isGameRunning()
        ) {
            serverController.sendMessageOnServerFold();
            gameStats.setPlayerActivity(this.PLAYER_NAME, false);
            gameView.setPlayerView(gameStats.getMainPlayer());
            gameStats.setPlayerPushedButton(true);
        }else
            gameView.showGameEventMessage("You can't do it now!",
                                        ViewControllerTimerConst.TIME_SHORT);
    }

    @Override
    public void checkButtonClicked() {
        if(gameStats.getLead().equals(this.PLAYER_NAME)
                && gameStats.getPlayerMoney(this.PLAYER_NAME)>=gameStats.getRate()
                && !gameStats.isPlayerPushedButton()
                && gameStats.isGameRunning()
        ) {
            serverController.sendMessageOnServerCheck();
            gameStats.onMeSpendMoney(gameStats.getRate());
            gameStats.setPlayerPushedButton(true);
            gameView.setPlayerView(gameStats.getMainPlayer());
        }else
            gameView.showGameEventMessage("You can't do it now!",
                    ViewControllerTimerConst.TIME_SHORT);
    }

    @Override
    public void raiseButtonClicked(int rate) {
        if(gameStats.getLead().equals(this.PLAYER_NAME)                 //если игрок- ведущий
                && gameStats.getPlayerMoney(this.PLAYER_NAME)>=rate     //если денег хватает на ставку
                && rate >= gameStats.getRate()                          //если указанная стака больше существующей
                && !gameStats.isPlayerPushedButton()                    //ксли игрок ещё ничего не делал в ходу
                && gameStats.isGameRunning()                            //если идёт игра
        ) {
            serverController.sendMessageOnServerRaise(rate);
            gameStats.setRate(rate);
            gameStats.onMeSpendMoney(rate);
            gameStats.setPlayerPushedButton(true);
            gameView.setRate(rate);
            gameView.setPlayerView(gameStats.getMainPlayer());
        }else
            gameView.showGameEventMessage("You can't do it now!",
                    ViewControllerTimerConst.TIME_SHORT);
    }

    @Override
    public int getPlayerMoney(){ return this.gameStats.getPlayerMoney(this.PLAYER_NAME); }

    @Override
    public void acceptMessageFromServerDidNotEnterLobby() {
        gameView.didNoteEnterLobbySoLeave();
    }

    @Override
    public void onViewStopped() {
        serverController.sendMessageOnServerLeave();
        PokerApplicationManager.getInstance()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(
                        Constants.PLAYER_MONEY
                        ,gameStats
                                .getPlayerByName(this.PLAYER_NAME)
                                .getMoney()
                ).apply();
    }

    public int getRate(){ return this.gameStats.getRate(); }

    @Override
    public void allInButtonClicked(){
        if(gameStats.getLead().equals(this.PLAYER_NAME)             //если игрок- ведущий
                && gameStats.getPlayerMoney(this.PLAYER_NAME)>=0    //если деньги есть
                && !gameStats.isPlayerPushedButton()                //если игрок ещё ничего не делал в ходу
                && gameStats.isGameRunning()                        //если идёт игра
        ) {
                serverController.sendMessageOnServerAllIn();
                gameStats.onMeSpendMoney(
                        gameStats
                                .getPlayerByName(this.PLAYER_NAME)
                                .getMoney()
                );
                gameView.setPlayerView(gameStats.getMainPlayer());
                gameStats.setPlayerPushedButton(true);
        }else
            gameView.showGameEventMessage("You can't do it now!",
                    ViewControllerTimerConst.TIME_SHORT);
    }

    @Override
    public void exitButtonClicked(){
        serverController.sendMessageOnServerLeave();
        //если игроков нет, то и деньги возвращать не нужно
        if(!gameStats.getPlayers().isEmpty())
            PokerApplicationManager.getInstance()
                    .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(
                            Constants.PLAYER_MONEY
                            , gameStats
                                    .getPlayerByName(this.PLAYER_NAME)
                                    .getMoney()
                    ).apply();

    }

    //То, что мы получаем от сервера
    @Override
    public void acceptMessageFromServerNewPlayerJoin(Player player) {
        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_FIRST))
            player.setPos(ViewControllerActionCode.POSITION_OPPONENT_FIRST);
        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_SECOND))
            player.setPos(ViewControllerActionCode.POSITION_OPPONENT_SECOND);
        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_THIRD))
            player.setPos(ViewControllerActionCode.POSITION_OPPONENT_THIRD);
        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_FOURTH))
            player.setPos(ViewControllerActionCode.POSITION_OPPONENT_FOURTH);
        gameStats.addNewPlayer(player);
        //вот это я сделал, чтобы избежать ошибок
        //если вот это gameStats.addNewPlayer(player); не пройдёт
        //то нам не покажется того, чего нет в gameStats
        player = gameStats.getPlayerByName(player.getName());
        gameView.setOpponentView(player.getPos(), player);

    }
    @Override
    public void acceptMessageFromServerOpponentCheck(String name, String newLead, boolean didRoundChange) {
        //если это мы, то никаких действий предпринимать не надо
        if(!name.equals(this.PLAYER_NAME))
            gameStats.onPlayerCheck(name);

        switch (gameStats.getPlayerByName(name).getPos()){
            case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                gameView.showFirstOpponentEventMessage("Check",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                gameView.showSecondOpponentEventMessage("Check",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                gameView.showThirdOpponentEventMessage("Check",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                gameView.showFourthOpponentEventMessage("Check",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            default:
                gameView.showGameEventMessage("You check!",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
        }


        if(didRoundChange) gameStats.increaseRoundsNum();
        checkIfShouldOpenNewCard();
        gameStats.setLead(newLead);
        //получаем место того игрока, который будет ходить следующим
        gameView.setLead(gameStats.getPlayerByName(newLead).getPos());
        gameView.setBank(gameStats.getBank());
        gameView.updatePlayerMoney(
                gameStats.getPlayerByName(name).getPos()
                ,gameStats.getPlayerByName(name).getMoney()
        );
        if(newLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentRaise(String name, Integer rate, String newLead, boolean didRoundChange) {
        gameStats.onPlayerRaise(name, rate);

        switch (gameStats.getPlayerByName(name).getPos()){
            case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                gameView.showFirstOpponentEventMessage("Raise"+ rate.toString(),
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                gameView.showSecondOpponentEventMessage("Raise"+ rate.toString(),
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                gameView.showThirdOpponentEventMessage("Raise"+ rate.toString(),
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                gameView.showFourthOpponentEventMessage("Raise"+ rate.toString(),
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            default:
                gameView.showGameEventMessage("You raise",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
        }

        if(didRoundChange) gameStats.increaseRoundsNum();
        checkIfShouldOpenNewCard();
        gameStats.setLead(newLead);
        gameView.setRate(rate);
        gameView.setLead(gameStats.getPlayerByName(newLead).getPos());
        gameView.setBank(gameStats.getBank());
        gameView.updatePlayerMoney(
                gameStats.getPlayerByName(name).getPos()
                ,gameStats.getPlayerByName(name).getMoney()
        );
        if(newLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentAllIn(String name, String newLead, boolean didRoundChange) {
        gameStats.onPlayerAllIn(name);
        if(didRoundChange) gameStats.increaseRoundsNum();
        checkIfShouldOpenNewCard();

        switch (gameStats.getPlayerByName(name).getPos()){
            case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                gameView.showFirstOpponentEventMessage("All in",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                gameView.showSecondOpponentEventMessage("All in",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                gameView.showThirdOpponentEventMessage("All in",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                gameView.showFourthOpponentEventMessage("All in",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            default:
                gameView.showGameEventMessage("You all in",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
        }

        gameStats.setLead(newLead);
        gameView.setLead(gameStats.getPlayerByName(newLead).getPos());
        gameView.setBank(gameStats.getBank());
        gameView.updatePlayerMoney(
                gameStats.getPlayerByName(name).getPos()
                ,gameStats.getPlayerByName(name).getMoney()
        );
        if(newLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentFold(String name, String newLead, boolean didRoundChange) {
        gameStats.onPlayerFold(name);

        switch (gameStats.getPlayerByName(name).getPos()){
            case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                gameView.showFirstOpponentEventMessage("Fold",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                gameView.showSecondOpponentEventMessage("Fold",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                gameView.showThirdOpponentEventMessage("Fold",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                gameView.showFourthOpponentEventMessage("Fold",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            default:
                gameView.showGameEventMessage("You fold",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
        }

        if(didRoundChange) gameStats.increaseRoundsNum();
        checkIfShouldOpenNewCard();
        gameStats.setLead(newLead);
        gameView.setLead(gameStats.getPlayerByName(newLead).getPos());
        gameView.setBank(gameStats.getBank());
        gameView.updatePlayerMoney(
                gameStats.getPlayerByName(name).getPos()
                ,gameStats.getPlayerByName(name).getMoney()
        );
        if(newLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentMeDidSomething(String nextLead, boolean didRoundChange){
        if(didRoundChange) gameStats.increaseRoundsNum();
        checkIfShouldOpenNewCard();
        gameStats.setLead(nextLead);
        gameView.setLead(gameStats.getPlayerByName(nextLead).getPos());
        gameView.setBank(gameStats.getBank());
        gameView.updatePlayerMoney(
                gameStats.getPlayerByName(this.PLAYER_NAME).getPos()
                ,gameStats.getPlayerByName(this.PLAYER_NAME).getMoney()
        );
        if(nextLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentLeft(String name, String newLead, boolean didRoundChange) {
        switch (gameStats.getPosPlayer(name)){
            case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                gameView.clearCards(ViewControllerActionCode.CLEAR_FIRST_OPPONENT_CARDS);
                gameView.clearOpponentView(ViewControllerActionCode.POSITION_OPPONENT_FIRST);
                gameView.showFirstOpponentEventMessage("Player left",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                gameView.clearCards(ViewControllerActionCode.CLEAR_SECOND_OPPONENT_CARDS);
                gameView.clearOpponentView(ViewControllerActionCode.POSITION_OPPONENT_SECOND);
                gameView.showSecondOpponentEventMessage("Player left",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                gameView.clearCards(ViewControllerActionCode.CLEAR_THIRD_OPPONENT_CARDS);
                gameView.clearOpponentView(ViewControllerActionCode.POSITION_OPPONENT_THIRD);
                gameView.showThirdOpponentEventMessage("Player left",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
            case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                gameView.clearCards(ViewControllerActionCode.CLEAR_FOURTH_OPPONENT_CARDS);
                gameView.clearOpponentView(ViewControllerActionCode.POSITION_OPPONENT_FOURTH);
                gameView.showFourthOpponentEventMessage("Player left",
                        ViewControllerTimerConst.TIME_SHORT);
                break;
        }
        //ВАЖНО: сначала надо очищать во вью,
        //а потом уже в логике
        //иначе gameStats.getPosPlayer(name) вернёт не то
        gameStats.deleteOpponent(name);
        if(didRoundChange) gameStats.increaseRoundsNum();
        gameStats.setLead(newLead);
        checkIfShouldOpenNewCard();
        if(newLead.equals(this.PLAYER_NAME)) gameStats.setPlayerPushedButton(false);
    }
    @Override
    public void acceptMessageFromServerOpponentStop(String name) {
        //тут можно сделать типа отображнеие того, что игрок отошел
        //а вообще пока всё равно
    }
    @Override
    public void acceptMessageFromServerOpponentRestore(String name) {
        //...
    }
    @Override
    public void acceptMessageFromServerEndGame(Integer winVal, List<String> winners) {
        HashMap<Integer, List<Integer>> playersCards = gameStats.getAllPlayersCards();
        gameView.showAllPlayersCards(gameStats.getAllPlayersCards());
        gameView.showGameEventMessage("GAME OVER!"
                ,ViewControllerTimerConst.TIME_VERY_LONG);

        //для каждого имени победителя находим соотв. игрока
        //и пишем радом с ним "Winner"
        winners.stream().map(x->gameStats.getPlayerByName(x)).forEach((x)->{
            switch (x.getPos()){
                case ViewControllerActionCode.POSITION_MAIN_PLAYER:
                    gameView.showGameEventMessage("You win!"
                            ,ViewControllerTimerConst.TIME_VERY_LONG);
                    break;
                case ViewControllerActionCode.POSITION_OPPONENT_FIRST:
                    gameView.showFirstOpponentEventMessage("Winner"
                            ,ViewControllerTimerConst.TIME_VERY_LONG);
                    break;
                case ViewControllerActionCode.POSITION_OPPONENT_SECOND:
                    gameView.showSecondOpponentEventMessage("Winner"
                            ,ViewControllerTimerConst.TIME_VERY_LONG);
                    break;
                case ViewControllerActionCode.POSITION_OPPONENT_THIRD:
                    gameView.showThirdOpponentEventMessage("Winner"
                            ,ViewControllerTimerConst.TIME_VERY_LONG);
                    break;
                case ViewControllerActionCode.POSITION_OPPONENT_FOURTH:
                    gameView.showFourthOpponentEventMessage("Winner"
                            ,ViewControllerTimerConst.TIME_VERY_LONG);
                    break;
            }
        });

        //когда начнётся след. игра, отсчитается таймер
        this.flagToWaitBeforeGameStarts = true;
    }

    @Override
    public void acceptMessageFromServerEnterLobby(
            List<Player> allplayers
            ,List<Player> gameplayers
            ,List<Integer> deck
            ,Map<String, List<Integer>> playersCardsMap
            ,String lead
            ,Integer base_rate
            ,Integer rounds_done
            ,Integer bank
            ,boolean isgamerunning
    ) {
        gameStats.setGame(
                allplayers
                ,gameplayers
                ,playersCardsMap
                ,this.PLAYER_NAME
                ,deck
                ,lead
                ,base_rate
                ,bank
                ,rounds_done
        );
        if(!isgamerunning) gameStats.setPlayerCards(gameStats.getMainPlayerName(), new ArrayList<>());
        gameView.setPlayerView(gameStats.getMainPlayer());

        gameStats.setPlayerPos(
                this.PLAYER_NAME
                ,ViewControllerActionCode.POSITION_MAIN_PLAYER
        );
        if(isgamerunning) {
            showFirstFreeCards();
            checkIfShouldOpenNewCard();
            gameView.setBank(bank);
            gameView.setLead(gameStats.getPosPlayer(gameStats.getLead()));
        }
        sitThePlayers();
    }
    @Override
    public void acceptMessageFromServerRestore(
            List<Player> allplayers
            ,List<Player> gameplayers
            ,List<Integer> deck
            ,Map<String, List<Integer>> playersCardsMap
            ,String lead
            ,Integer rate
            ,Integer rounds_done
            ,Integer bank
            ,boolean isgamerunning
    ) {
        if(isgamerunning) gameView.setBank(bank);
        gameStats.setGame(
                allplayers
                ,gameplayers
                ,playersCardsMap
                ,this.PLAYER_NAME
                ,deck
                ,lead
                ,rate
                ,bank
                ,rounds_done
        );
        if (isgamerunning) {
            gameView.setRate(rate);
            showFirstFreeCards();
            checkIfShouldOpenNewCard();
        }
        sitThePlayers();
    }
    @Override
    public void acceptMessageFromServerGameStarts(
            List<Player> allplayers
            ,List<Player> gameplayers
            ,List<Integer> deck
            ,Map<String,List<Integer>> playersCardsMap
            ,String lead
            ,Integer base_rate
    ) {
        if(flagToWaitBeforeGameStarts)
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        gameView.clearAll();
                    }catch (Exception e){}
                    Logger.getAnonymousLogger().info("Starting game..."+allplayers.size());
                    gameStats.setGame(
                            allplayers
                            ,allplayers
                            ,playersCardsMap
                            ,PLAYER_NAME
                            ,deck
                            ,lead
                            ,base_rate
                            ,0
                            ,0
                    );
                    gameView.setPlayerView(gameStats.getMainPlayer());
                    gameView.setBank(0);
                    showFirstFreeCards();
                    sitThePlayers();
                    gameView.setRate(base_rate);
                    gameView.setLead(gameStats.getPosPlayer(gameStats.getLead()));
                    gameView.setHandPowerProgressBarProgress(gameStats.getMainPlayerHandCombinationRank(3));
                    serverController.sendMessageOnServerHandPower(gameStats.getMainPlayerHandPower(5));
                }
            }, ViewControllerTimerConst.TIME_SHORT);
        else {
            Logger.getAnonymousLogger().info("Starting game..." + allplayers.size());
            gameStats.setGame(
                    allplayers
                    , allplayers
                    , playersCardsMap
                    , PLAYER_NAME
                    , deck
                    , lead
                    , base_rate
                    , 0
                    , 0
            );
            gameView.setPlayerView(gameStats.getMainPlayer());
            gameView.setBank(0);
            showFirstFreeCards();
            sitThePlayers();
            gameView.setRate(base_rate);
            gameView.setLead(gameStats.getPosPlayer(gameStats.getLead()));
            gameView.setHandPowerProgressBarProgress(gameStats.getMainPlayerHandCombinationRank(3));
            serverController.sendMessageOnServerHandPower(gameStats.getMainPlayerHandPower(5));
        }
    }

    private void showFirstFreeCards(){
        gameView.setCardView(
                ViewControllerActionCode.ADD_COMMUNITY_CARD_FIRST
                ,gameStats.getDeck().get(0)
        );
        gameView.setCardView(
                ViewControllerActionCode.ADD_COMMUNITY_CARD_SECOND
                ,gameStats.getDeck().get(1)
        );
        gameView.setCardView(
                ViewControllerActionCode.ADD_COMMUNITY_CARD_THIRD
                ,gameStats.getDeck().get(2)
        );
    }

    private void sitThePlayers(){
        Logger.getAnonymousLogger().info("Sitting the players, their number is: "+gameStats.getPlayers().size());
        this.gameStats.getPlayers().forEach((i) -> {
                    if (!i.getName().equals(this.PLAYER_NAME)) {
                        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_FIRST))
                            i.setPos(ViewControllerActionCode.POSITION_OPPONENT_FIRST);
                        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_SECOND))
                            i.setPos(ViewControllerActionCode.POSITION_OPPONENT_SECOND);
                        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_THIRD))
                            i.setPos(ViewControllerActionCode.POSITION_OPPONENT_THIRD);
                        if (gameStats.checkIfPlaceIsNotTaken(ViewControllerActionCode.POSITION_OPPONENT_FOURTH))
                            i.setPos(ViewControllerActionCode.POSITION_OPPONENT_FOURTH);
                        Logger.getAnonymousLogger().info("the player is placed: " + i.getName()+"  "+i.getPos());
                        gameView.setOpponentView(i.getPos(), i);
                    }
                }
        );
    }


    //короче, очень полезные метод
    //смотрит, изменился ли раунд с его последнего вызова
    //если да,открывает, то скольно надо

    //типа мы будем при каждом действии спрашивать себя
    // "а не надо ли мне открыть ещё одну community card?"
    private void checkIfShouldOpenNewCard(){
        int roundNow = gameStats.getRoundsNum();
        int cardsOpened = gameStats.getCardsOpened();

        if(roundNow >= 1 && cardsOpened < 4) {
            gameView.setCardView(
                    ViewControllerActionCode.ADD_COMMUNITY_CARD_FOURTH
                    , gameStats.getDeck().get(3)
            );
            gameStats.setCardsOpened(4);
            gameView.setHandPowerProgressBarProgress(gameStats.getMainPlayerHandCombinationRank(4));
        }
        if(roundNow >= 2 && cardsOpened < 5) {
            gameView.setCardView(
                    ViewControllerActionCode.ADD_COMMUNITY_CARD_FIFTH
                    , gameStats.getDeck().get(4)
            );
            gameStats.setCardsOpened(5);
            gameView.setHandPowerProgressBarProgress(gameStats.getMainPlayerHandCombinationRank(5));
        }
    }
}