import Point from "@arcgis/core/geometry/Point";
import Polyline from "@arcgis/core/geometry/Polyline";

export const pointConnectsToLoxodromeLineFollowingSetBearing = (point: Point, lines: LineWithNavigationTypeAndId[]): LineWithSetBearingAndId | undefined => {
    let pointConnectsToStartOfLine: boolean;
    const connectingLine = lines.find((lineWrapper: LineWithNavigationTypeAndId) => {
        if (lineWrapper.navigationType !== NavigationType.LOXODROME) {
            return false;
        }
        const line = lineWrapper.line;
        const startPoint = line.getPoint(0, 0);
        const endPoint = line.getPoint(0, line.paths[0].length - 1);

        if (point.equals(startPoint)) {
            pointConnectsToStartOfLine = true;
            return true;
        } else if (point.equals(endPoint)) {
            pointConnectsToStartOfLine = false;
            return true;
        } else {
            return false;
        }

    })

    if (!connectingLine) {
        return undefined;
    }

    const line = connectingLine.line;
    let pointA: Point;
    let pointB: Point;
    if (pointConnectsToStartOfLine) {
        pointA = line.getPoint(0, 0);
        pointB = line.getPoint(0, 1);
    } else {
        pointA = line.getPoint(0, line.paths[0].length - 1);
        pointB = line.getPoint(0, line.paths[0].length - 2);
    }
    const setBearing = lineFollowsSetBearing(pointA, pointB);

    if (!setBearing) {
        return undefined;
    }

    return {
        line: line,
        setBearing: setBearing,
        id: connectingLine.id
    }
}

const lineFollowsSetBearing = (pointA: Point, pointB: Point): SetBearing | undefined => {
    if (pointA.x === pointB.x) {
        return SetBearing.LATITUDE;
    }
    if (pointA.y === pointB.y) {
        return SetBearing.LONGITUDE;
    }
    return undefined;
}

export enum SetBearing {
    LATITUDE = "LATITUDE",
    LONGITUDE = "LONGITUDE",
}

export enum NavigationType {
    LOXODROME = "LOXODROME",
    GEODESIC = "GEODESIC"
}

export type LineWithNavigationTypeAndId = {
    line: Polyline;
    navigationType: NavigationType;
    id: number;
}

export type LineWithSetBearingAndId = {
    line: Polyline;
    setBearing: SetBearing
    id: number;
}

const lineWithNavigationTypeFrom = (polyline: Polyline, navigationType: NavigationType, id: number): LineWithNavigationTypeAndId => {
    return {
        line: polyline,
        navigationType: navigationType,
        id: id
    };
}


//code for manually testing:
const tester = () => {
    //EsriJson polylines obtained from migrating oracle shape with test case = `GISA-38`
    const lineLoxodromeEsri1 = "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.8,53.8722222222222],[2.8963375,53.8666666666667]]]}"
    const lineLoxodromeEsri2 = "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.8,53.8333333333333],[2.8,53.8722222222222]]]}"
    const lineLoxodromeEsri3 = "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.90648611111111,53.8333333333333],[2.8,53.8333333333333]]]}"
    const lineGeodesicEsri = "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.8963375,53.8666666666667],[2.896471139098836,53.866228076964944],[2.8966047754020248,53.86578948708199],[2.8967384089096697,53.86535089701787],[2.896872039621873,53.86491230677257],[2.897005667538736,53.8644737163461],[2.8971392926603623,53.86403512573847],[2.8972729149868544,53.86359653494969],[2.8974065345183138,53.86315794397976],[2.8975401512548435,53.862719352828684],[2.8976737651965454,53.86228076149646],[2.8978073763435224,53.861842169983106],[2.8979409846958766,53.86140357828862],[2.8980745902537093,53.860964986413],[2.898208193017125,53.86052639435628],[2.8983417929862245,53.860087802118436],[2.8984753901611104,53.85964920969948],[2.8986089845418848,53.859210617099414],[2.8987425761286514,53.858772024318256],[2.8988761649215107,53.858333431356016],[2.899009750920566,53.85789483821267],[2.8991433341259194,53.857456244888255],[2.8992769145376736,53.85701765138276],[2.8994104921559307,53.856579057696194],[2.8995440669807926,53.85614046382856],[2.8996776390123613,53.85570186977986],[2.89981120825074,53.85526327555011],[2.8999447746960305,53.854824681139306],[2.9000783383483353,53.854386086547464],[2.900211899207757,53.85394749177456],[2.9003454572743967,53.85350889682066],[2.900479012548358,53.8530703016857],[2.9006125650297423,53.85263170636973],[2.9007461147186517,53.85219311087272],[2.9008796616151895,53.85175451519472],[2.901013205719457,53.851315919335704],[2.901146747031557,53.85087732329569],[2.901280285551591,53.85043872707467],[2.9014138212796623,53.85000013067266],[2.9015473542158716,53.84956153408966],[2.9016808843603235,53.84912293732568],[2.9018144117131177,53.84868434038073],[2.9019479362743574,53.84824574325481],[2.902081458044145,53.84780714594792],[2.9022149770225836,53.84736854846006],[2.9023484932097734,53.84692995079126],[2.902482006605818,53.8464913529415],[2.902615517210819,53.8460527549108],[2.902749025024878,53.845614156699156],[2.9028825300480987,53.84517555830658],[2.9030160322805827,53.844736959733076],[2.9031495317224314,53.84429836097865],[2.903283028373748,53.8438597620433],[2.903416522234634,53.84342116292704],[2.903550013305192,53.842982563629874],[2.903683501585524,53.84254396415181],[2.9038169870757313,53.842105364492845],[2.9039504697759173,53.841666764652985],[2.9040839496861834,53.841228164632234],[2.904217426806632,53.84078956443061],[2.904350901137365,53.84035096404811],[2.904484372678485,53.839912363484736],[2.9046178414300936,53.839473762740504],[2.904751307392293,53.83903516181541],[2.9048847705651855,53.83859656070946],[2.9050182309488735,53.83815795942266],[2.9051516885434583,53.83771935795502],[2.905285143349042,53.83728075630654],[2.9054185953657274,53.83684215447721],[2.9055520445936165,53.83640355246707],[2.905685491032811,53.83596495027609],[2.9058189346834133,53.83552634790432],[2.9059523755455254,53.83508774535172],[2.906085813619249,53.83464914261831],[2.9062192489046863,53.8342105397041],[2.906352681401939,53.8337719366091],[2.90648611111111,53.8333333333333]]]}"
    const lineLoxodrome1 = Polyline.fromJSON(JSON.parse(lineLoxodromeEsri1));
    const lineLoxodrome2 = Polyline.fromJSON(JSON.parse(lineLoxodromeEsri2));
    const lineLoxodrome3 = Polyline.fromJSON(JSON.parse(lineLoxodromeEsri3));
    const lineGeodesic = Polyline.fromJSON(JSON.parse(lineGeodesicEsri));
    const lines =[
        lineWithNavigationTypeFrom(lineLoxodrome1, NavigationType.LOXODROME,1),
        lineWithNavigationTypeFrom(lineLoxodrome2, NavigationType.LOXODROME, 2),
        lineWithNavigationTypeFrom(lineLoxodrome3, NavigationType.LOXODROME, 3),
        lineWithNavigationTypeFrom(lineGeodesic, NavigationType.GEODESIC, 4),
    ];

    //Connects to loxodrome following a latitude
    const geodesicPoint = lineGeodesic.getPoint(0, lineGeodesic.paths[0].length - 1);

    //Connects to loxodrome that doesn't follow any set bearing
    // const geodesicPoint = lineGeodesic.getPoint(0, 0);

    const result = pointConnectsToLoxodromeLineFollowingSetBearing(geodesicPoint, lines);
    console.log(result);
}

tester();