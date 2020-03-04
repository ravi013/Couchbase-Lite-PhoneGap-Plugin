#import <Cordova/CDV.h>
#import "CBLite.h"
#import "CouchbaseLite.h"
#import "CBLListener.h"

@interface CBLite : CDVPlugin
    

@property (nonatomic, strong) NSURL *liteURL;
@property (nonatomic, strong) CBLDatabase *database;
@property (nonatomic, strong) CBLReplication *pull ;
@property (nonatomic, strong) NSString *remoteUrl;
@property (nonatomic, strong) CBLListener *listener ;

- (void)getURL:(CDVInvokedUrlCommand*)urlCommand;
- (void) startReplication;

@end

