# League Restream Tool
This tool is designed to help the [ALTTPRLeauge](https://alttprleague.com/) with restreaming matches to twitch using obs.

## Initial Setup
1. In the `output/` directory there will be the following files you need to link up to obs.

| file                | location                        |
|---------------------|---------------------------------|
| PostRaceLayout.html | PostRace Scene "PostRaceLayout" |
| PreRaceLayout.html  | PreRace Scene "PreRaceLayout"   |
| RaceLayout.html     | 2P & 4P Scene "WebLayout"       |

2. Set up the obs <> restream tool link 
   - in OBS `Tools > obs-websocket Settings`
     - Check `Enable WebSocket server`
     - Choose a port. default 8456, but you may change it if you wish.
     - Click `Generate Password` to create a unique password.
   - Restream Tool `Settings > OBS Connection Settings`
     - fill ou the dialog window that appears matching the settings from the step above.
       - server will most likely be the default of `localhost`.

3. enter your Restreamer code from the [Restreamer Dashboard](https://alttprleague.com/restream/) into `Settings > Restreamer Auth Key`

## Per Race Setup

1. From the [Restreamer Dashboard](https://alttprleague.com/restream/) find and copy the `ID` from the list of upcoming matches.
2. In the sidebar select the correct channel that you will be streaming to
3. Paste the `ID` into the `SG Episode ID` box and click `update`
4. Make any changes to the racer's alt streams, and crew's twitch/display names as needed.
5. In the Restream Tool make sure you are connected to OBS, You should have a green light in the bottom left corner.
6. Select the corresponding channel to load in the current races' information.
7. In OBS refresh any of the Layout browser sources, and racer media streams you need.