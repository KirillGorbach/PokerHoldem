package com.poker.holdem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.poker.holdem.constants.Constants;
import com.poker.holdem.logic.player.Player;
import com.poker.holdem.server.deserialization.MyDeserializer;
import com.poker.holdem.server.deserialization.endgame.EndgameResp;
import com.poker.holdem.server.deserialization.enterlobby.EnterResp;
import com.poker.holdem.server.deserialization.gamestarts.GameStartsResp;
import com.poker.holdem.server.deserialization.newplayerjoin.NewPlayerJoinResp;
import com.poker.holdem.server.deserialization.playerallin.PlayerAllInResp;
import com.poker.holdem.server.deserialization.playercheck.PlayerCheckResp;
import com.poker.holdem.server.deserialization.playerfold.PlayerFoldResp;
import com.poker.holdem.server.deserialization.playerleft.PlayerLeftResp;
import com.poker.holdem.server.deserialization.playerraise.PlayerRaiseResp;
import com.poker.holdem.server.deserialization.restore.RestoreResp;
import com.poker.holdem.server.deserialization.youallin.YouAllInResp;
import com.poker.holdem.server.deserialization.youcheck.YouCheckResp;
import com.poker.holdem.server.deserialization.youfold.YouFoldResp;
import com.poker.holdem.server.deserialization.youraise.YouRaiseResp;
import com.poker.holdem.server.serialization.GetJSON;

import java.net.URISyntaxException;
import java.util.logging.Logger;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ServerController implements GameContract.Server {

    private GameContract.Presenter presenter;

    private Socket socket;
    private SharedPreferences prefs;

    private String SESSION_TOKEN;
    private String PLAYER_NAME;
    private String ROOM_NAME = "";
    private static  boolean toLog = true;

    ServerController(GameContract.Presenter presenter){

        this.SESSION_TOKEN = PokerApplicationManager
                .getInstance()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.SESSION_TOKEN, "");
        this.PLAYER_NAME = PokerApplicationManager
                .getInstance()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.PLAYER_NAME, "");

        this.presenter = presenter;

        this.socket = PokerApplicationManager.getInstance().getSocket();
        this.socket.on("enterlobby", onEnterLobby);
        this.socket.on("newplayerjoinedlobby", onNewPlayerJoin);
        this.socket.on("gamestarts", onGameStarts);
        this.socket.on("youcheck", onYouCheck);
        this.socket.on("playercheck", onPlayerCheck);
        this.socket.on("youraise", onYouRaise);
        this.socket.on("playerraise", onPlayerRaise);
        this.socket.on("youfold", onYouFold);
        this.socket.on("playerfold", onPlayerFold);
        this.socket.on("youallin", onYouAllIn);
        this.socket.on("playerallin", onPlayerAllIn);
        this.socket.on("youleft", onYouLeft);
        this.socket.on("playerleft", onPlayerLeft);
        this.socket.on("playerstops", onPlayerStops);
        this.socket.on("restore", onRestore);
        this.socket.on("playerrestores", onPlayerRestore);
        this.socket.on("endgame", onEndGame);
        this.socket.connect();
    }

    public void disconnect(){
        this.socket = PokerApplicationManager.getInstance().getSocket();
        this.socket.off("enterlobby", onEnterLobby);
        this.socket.off("newplayerjoinedlobby", onNewPlayerJoin);
        this.socket.off("gamestarts", onGameStarts);
        this.socket.off("youcheck", onYouCheck);
        this.socket.off("playercheck", onPlayerCheck);
        this.socket.off("youraise", onYouRaise);
        this.socket.off("playerraise", onPlayerRaise);
        this.socket.off("youfold", onYouFold);
        this.socket.off("playerfold", onPlayerFold);
        this.socket.off("youallin", onYouAllIn);
        this.socket.off("playerallin", onPlayerAllIn);
        this.socket.off("youleft", onYouLeft);
        this.socket.off("playerleft", onPlayerLeft);
        this.socket.off("playerstops", onPlayerStops);
        this.socket.off("restore", onRestore);
        this.socket.off("playerrestores", onPlayerRestore);
        this.socket.off("endgame", onEndGame);
        this.socket.disconnect();
    }

    private Emitter.Listener onEnterLobby = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                //Logger.getAnonymousLogger().info("<-resp: "+args[0].toString());
                EnterResp enterResp = MyDeserializer.desEnterLobbyResponce(args[0].toString());
                //Logger.getAnonymousLogger().info("<-server: "+enterResp.getLobbyinfo().getAllplayers().get(0).getPlayername());
                if (enterResp.getDidenter())
                    presenter.acceptMessageFromServerEnterLobby(
                            enterResp.getAllAsPlayers()
                            ,enterResp.getGamePlayersAsPlayers()
                            ,enterResp.getLobbyinfo().getCards().getDeck()
                            ,enterResp.getPlayersCardsAsMap()
                            ,enterResp.getLobbyinfo().getLead()
                            ,enterResp.getLobbyinfo().getRate()
                            ,enterResp.getLobbyinfo().getRounds_done()
                            ,enterResp.getLobbyinfo().getBank()
                            ,enterResp.getLobbyinfo().getIsgamerunning()
                    );
                else {
                    Logger.getAnonymousLogger().info("<-----Didn't manage to Enter!");
                    presenter.acceptMessageFromServerDidNotEnterLobby();
                }
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onGameStarts = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                GameStartsResp gameStartsResp = MyDeserializer.desGameStartsResponce(args[0].toString());
                presenter.acceptMessageFromServerGameStarts(
                        gameStartsResp.getAllAsPlayers()
                        ,gameStartsResp.getGamePlayersAsPlayers()
                        ,gameStartsResp.getRoomparams().getCards().getDeck()
                        ,gameStartsResp.getPlayersCardsAsMap()
                        ,gameStartsResp.getLead()
                        ,gameStartsResp.getRoomparams().getRate()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }

        }
    };
    private Emitter.Listener onNewPlayerJoin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                NewPlayerJoinResp newPlayerJoinResp = MyDeserializer.desNewPlayerJoinResp(args[0].toString());
                Player player = new Player();
                player.setMoney(newPlayerJoinResp.getMoney());
                player.setName(newPlayerJoinResp.getName());
                player.setNumOfPicture(newPlayerJoinResp.getPicture());
                presenter.acceptMessageFromServerNewPlayerJoin(player);
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerAllIn = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                PlayerAllInResp playerAllInResp = MyDeserializer.desPlayerAllInResp(args[0].toString());
                presenter.acceptMessageFromServerOpponentAllIn(
                        playerAllInResp.getName()
                        , playerAllInResp.getNewlead()
                        , playerAllInResp.getNewround()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerCheck = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                PlayerCheckResp playerCheckResp = MyDeserializer.desPlayerCheckResp(args[0].toString());
                presenter.acceptMessageFromServerOpponentCheck(
                        playerCheckResp.getName()
                        , playerCheckResp.getNewlead()
                        , playerCheckResp.getNewround()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerFold = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                PlayerFoldResp playerFoldResp = MyDeserializer.desPlayerFoldResp(args[0].toString());
                presenter.acceptMessageFromServerOpponentFold(
                        playerFoldResp.getName()
                        , playerFoldResp.getNewlead()
                        , playerFoldResp.getNewround()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerLeft = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                PlayerLeftResp playerLeftResp = MyDeserializer.desPlayerLeftResp(args[0].toString());
                presenter.acceptMessageFromServerOpponentLeft(
                        playerLeftResp.getName()
                        , playerLeftResp.getNewlead()
                        , playerLeftResp.getNewround()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerRaise = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                PlayerRaiseResp playerRaiseResp = MyDeserializer.desPlayerRaiseResp(args[0].toString());
                presenter.acceptMessageFromServerOpponentRaise(
                        playerRaiseResp.getName()
                        , playerRaiseResp.getRate()
                        , playerRaiseResp.getNewlead()
                        ,playerRaiseResp.getNewround()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerStops = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                String name = MyDeserializer.desPlayerStopsRespName(args[0].toString());
                presenter.acceptMessageFromServerOpponentStop(name);
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onPlayerRestore = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                String name = MyDeserializer.desPlayerRestoresRespName(args[0].toString());
                presenter.acceptMessageFromServerOpponentRestore(name);
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onRestore = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                RestoreResp restoreResp = MyDeserializer.desRestoreResp(args[0].toString());

                //при восстановлении выдаётся новый токен, который нужно записать
                PokerApplicationManager
                        .getInstance()
                        .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(Constants.SESSION_TOKEN
                                ,restoreResp.getToken()
                        ).apply();
                if (restoreResp.getDidrestore())
                    presenter.acceptMessageFromServerRestore(
                            restoreResp.getAllAsPlayers()
                            ,restoreResp.getGamePlayersAsPlayers()
                            ,restoreResp.getRoomparams().getCards().getDeck()
                            ,restoreResp.getPlayersCardsAsMap()
                            ,restoreResp.getRoomparams().getLead()
                            ,restoreResp.getRoomparams().getRate()
                            ,restoreResp.getRoomparams().getRounds_done()
                            ,restoreResp.getRoomparams().getBank()
                            ,restoreResp.getRoomparams().getIsgamerunning()
                    );
                else
                    Logger.getAnonymousLogger().info("<-----Didn't manage to Restore!");
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };
    private Emitter.Listener onYouAllIn = args -> {
        try{
            if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
            YouAllInResp youAllInResp = MyDeserializer.desYouAllInResp(args[0].toString());
            if(!youAllInResp.getFlag()){
                Logger.getAnonymousLogger().info("<-----Didn't manage to AllIn!");
            }
        }catch (Exception e){
            //sendMessageOnServerLeave();
            e.printStackTrace();
        }
    };
    private Emitter.Listener onYouCheck = args -> {
        try{
            if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
            YouCheckResp youCheckResp = MyDeserializer.desYouCheckResp(args[0].toString());
            if(!youCheckResp.getFlag()){
                Logger.getAnonymousLogger().info("<-----Didn't manage to Check!");
            }
        }catch (Exception e){
            //sendMessageOnServerLeave();
            e.printStackTrace();
        }
    };
    private Emitter.Listener onYouFold = args -> {
        try{
            if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
            YouFoldResp youFoldResp = MyDeserializer.desYouFoldResp(args[0].toString());
            if(!youFoldResp.getFlag()){
                Logger.getAnonymousLogger().info("<-----Didn't manage to Fold!");
            }
        }catch (Exception e){
            //sendMessageOnServerLeave();
            e.printStackTrace();
        }
    };
    private Emitter.Listener onYouRaise = args -> {
        try{
            if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
            YouRaiseResp youRaiseResp = MyDeserializer.desYouRaiseResp(args[0].toString());
            if(!youRaiseResp.getFlag()){
                Logger.getAnonymousLogger().info("<-----Didn't manage to Raise!");
            }
        }catch (Exception e){
            //sendMessageOnServerLeave();
            e.printStackTrace();
        }
    };
    private Emitter.Listener onYouLeft = args -> {
        try{
            if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
            Integer myMoney = MyDeserializer.desYouLeft(args[0].toString());
            Logger.getAnonymousLogger().info("<--------In controller. I quit. My money: "+myMoney);
        }catch (Exception e){
            //sendMessageOnServerLeave();
            e.printStackTrace();
        }

    };
    private Emitter.Listener onEndGame = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                if (toLog) Logger.getAnonymousLogger().info("<-"+args[0].toString());
                EndgameResp endgameResp = MyDeserializer.desEndgameResp(args[0].toString());
                presenter.acceptMessageFromServerEndGame(
                        endgameResp.getWinVal()
                        ,endgameResp.getWinners()
                );
            }catch (Exception e){
                //sendMessageOnServerLeave();
                e.printStackTrace();
            }
        }
    };

    @Override
    public void sendMessageOnServerFold() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("fold"
                ,GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }

    @Override
    public void sendMessageOnServerCheck() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("check"
                ,GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }

    @Override
    public void sendMessageOnServerRaise(int rate) {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("raise"
                ,GetJSON.nameLobbynameTokenRate(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                        ,rate
                )
        );
    }

    @Override
    public void sendMessageOnServerAllIn() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("allin"
                ,GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }

    @Override
    public void sendMessageOnServerLeave() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("leavelobby"
                ,GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
        disconnect();
    }

    @Override
    public void sendMessageOnServerStop() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("stop"
                ,GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }

    @Override
    public void sendMessageOnServerRestore() {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("restore"
                ,GetJSON.nameLobbynameToken(
                        this.ROOM_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }

    @Override
    public void sendMessageOnServerHandPower(long power) {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        socket.emit("myhandpower"
                ,GetJSON.nameTokenPower(
                        this.PLAYER_NAME
                        ,this.SESSION_TOKEN
                        ,power
                )
        );
    }

    @Override
    public void sendMessageOnServerEnterLobby(String roomName) {
        if (toLog) Logger.getAnonymousLogger().info("<-sending");
        this.ROOM_NAME = roomName;
        socket.emit("enterlobby",
                GetJSON.nameLobbynameToken(
                        this.PLAYER_NAME
                        ,this.ROOM_NAME
                        ,this.SESSION_TOKEN
                )
        );
    }
}
